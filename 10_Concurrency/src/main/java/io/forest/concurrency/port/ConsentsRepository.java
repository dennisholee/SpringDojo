package io.forest.concurrency.port;

import java.util.Optional;
import java.util.UUID;

import io.forest.concurrency.port.dto.ConsentDTO;

public interface ConsentsRepository {

	Optional<ConsentDTO> findById(UUID id);
}
