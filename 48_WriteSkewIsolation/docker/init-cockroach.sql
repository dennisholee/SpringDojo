CREATE DATABASE IF NOT EXISTS cdm;
USE cdm;

-- A signatory is a governance sub-entity of a CDM party (customer).
CREATE TABLE IF NOT EXISTS signatory (
    id         STRING PRIMARY KEY,
    party_id   STRING NOT NULL,
    authorized BOOL   NOT NULL
);

-- The hot row a snapshot-isolated engine needs in order to close the write-skew gap in
-- application code. CockroachDB is strict serializable, so no scenario ever touches it; it
-- exists so both adapters implement the same Tx surface.
CREATE TABLE IF NOT EXISTS party_counter (
    party_id STRING PRIMARY KEY,
    seq      INT    NOT NULL
);

-- Reset for a deterministic start: party P1, exactly two authorized signatories.
DELETE FROM signatory;
INSERT INTO signatory (id, party_id, authorized) VALUES
    ('alice', 'P1', true),
    ('bob',   'P1', true);

DELETE FROM party_counter;
INSERT INTO party_counter (party_id, seq) VALUES ('P1', 0);
