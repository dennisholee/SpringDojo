package io.forest.curator.port;

import java.util.List;
import java.util.Optional;

import io.forest.curator.common.model.Status;
import io.forest.curator.port.dto.OutboxDto;

public interface OutboxRepository {

	OutboxDto create(OutboxDto taskDto);

	List<OutboxDto> findByStatus(Status taskStatus);

	Optional<OutboxDto> save(OutboxDto taskDTO);

}
