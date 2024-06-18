package io.forest.txnoutbox.adapter.repository.sql;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.forest.txnoutbox.adapter.repository.sql.model.Outbox;

public interface OutboxJpaRepository extends JpaRepository<Outbox, UUID> {

}
