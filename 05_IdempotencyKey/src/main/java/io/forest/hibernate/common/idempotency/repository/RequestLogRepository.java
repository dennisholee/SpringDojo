package io.forest.hibernate.common.idempotency.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestLogRepository extends R2dbcRepository<RequestLog, UUID>{

}
