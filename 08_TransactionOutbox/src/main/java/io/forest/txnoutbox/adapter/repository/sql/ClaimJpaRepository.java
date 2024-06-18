package io.forest.txnoutbox.adapter.repository.sql;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.forest.txnoutbox.adapter.repository.sql.model.Claim;

public interface ClaimJpaRepository extends JpaRepository<Claim, UUID> {

}
