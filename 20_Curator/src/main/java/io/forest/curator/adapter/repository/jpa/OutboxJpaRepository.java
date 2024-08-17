package io.forest.curator.adapter.repository.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.forest.curator.adapter.repository.jpa.model.Outbox;
import io.forest.curator.common.model.Status;

@Repository
public interface OutboxJpaRepository extends JpaRepository<Outbox, UUID> {

	List<Outbox> findByStatus(Status taskStatus);
}
