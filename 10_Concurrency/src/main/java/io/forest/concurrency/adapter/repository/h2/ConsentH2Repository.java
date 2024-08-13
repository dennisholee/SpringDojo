package io.forest.concurrency.adapter.repository.h2;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.forest.concurrency.adapter.repository.model.ConsentEntity;
import io.forest.concurrency.domain.model.Consent;

public interface ConsentH2Repository extends JpaRepository<ConsentEntity, UUID> {

}
