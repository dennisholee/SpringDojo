package io.forest.cdm.readmodel.internal;

import io.forest.cdm.readmodel.Customer360View;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for the {@code customer_360} projection, owned by the {@code readmodel} module and
 * implemented by {@link JdbcProjectionRepository}.
 */
public interface ProjectionRepository {

    void upsert(Customer360View view);

    Optional<Customer360View> find(UUID partyId);

    long count();
}
