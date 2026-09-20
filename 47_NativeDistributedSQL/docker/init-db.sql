-- ============================================================================
-- CDM "Native Distributed SQL" PoC schema.
--
-- Executed once, against a fresh cluster, by the `init` service in
-- docker/docker-compose.yml. Safe to re-run only on a fresh cluster: the
-- ALTER DATABASE ... REGION statements are not idempotent.
--
-- Layout: two regions, `uk` (primary) and `hk`.
--   Core Domains   (UK): party, relationship, contact_point
--   Market Domain  (HK): product_holding
-- ============================================================================

-- 1. Multi-region database. `uk` is primary because the Core Domains are UK-mastered.
--    The region names must match the `region=` labels in the node localities.
CREATE DATABASE IF NOT EXISTS cdm;
ALTER DATABASE cdm SET PRIMARY REGION "uk";
ALTER DATABASE cdm ADD REGION "hk";

-- 2. Survival goal stays at its default, ZONE.
--    ZONE is what keeps every row's replicas inside the row's own region, which is the residency
--    property this PoC asserts. REGION survival is not available in this topology: surviving the
--    loss of a whole region needs a Raft quorum that can live outside it, which requires at least
--    three regions. That is a real finding for the programme - see docs/adr/ADR-001.
--    (Confirmed against a running cluster: SHOW SURVIVAL GOAL FROM DATABASE cdm => zone.)

USE cdm;

-- 3. Core Domains - UK. `REGIONAL BY ROW` adds an implicit `crdb_region` column; the
--    application always writes 'uk' for these tables, so the rows never leave the UK.
CREATE TABLE IF NOT EXISTS party (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    party_type    STRING      NOT NULL,
    legal_name    STRING      NOT NULL,
    master_region STRING      NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
) LOCALITY REGIONAL BY ROW;

-- 4. A party's relationship to a market / line of business (e.g. UK_RETAIL, HK_INSURANCE).
--    Mastered in the UK even when it describes an HK market.
CREATE TABLE IF NOT EXISTS relationship (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    party_id         UUID        NOT NULL,
    market           STRING      NOT NULL,
    line_of_business STRING      NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
) LOCALITY REGIONAL BY ROW;

-- 5. Contact points (address, email, phone) for a party.
CREATE TABLE IF NOT EXISTS contact_point (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    party_id      UUID        NOT NULL,
    contact_type  STRING      NOT NULL,
    contact_value STRING      NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
) LOCALITY REGIONAL BY ROW;

-- 6. Market Domain - HK. `REGIONAL BY ROW` so a future market (e.g. SG) can be homed in its
--    own region without a schema change; today the application always writes 'hk'.
CREATE TABLE IF NOT EXISTS product_holding (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    party_id       UUID        NOT NULL,
    product_type   STRING      NOT NULL,
    account_number STRING      NOT NULL,
    market         STRING      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
) LOCALITY REGIONAL BY ROW;

-- 7. Referential integrity is also enforced in the application layer inside the transaction
--    (see ProductHoldingService), because cross-region foreign keys between REGIONAL BY ROW
--    tables have placement caveats. This unique index is what turns a duplicate account into a
--    real database violation, which scenario B2 uses to prove a mid-transaction abort rolls the
--    UK writes back too. CockroachDB requires unique indexes on REGIONAL BY ROW tables to
--    include crdb_region.
CREATE UNIQUE INDEX IF NOT EXISTS product_holding_account_uq
    ON product_holding (crdb_region, account_number);

-- 8. Pattern C read model: a denormalised customer-360 projection, maintained asynchronously and
--    owned by the readmodel module. Deliberately a plain table here; production would add
--    LOCALITY GLOBAL so every region can read it without a cross-region hop.
CREATE TABLE IF NOT EXISTS customer_360 (
    party_id         UUID        NOT NULL,
    legal_name       STRING      NOT NULL,
    market           STRING,
    line_of_business STRING,
    contact_point    STRING,
    account_numbers  STRING,
    projected_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (party_id)
);

-- 9. Show the resulting placement policy for the record.
SHOW REGIONS FROM DATABASE cdm;
