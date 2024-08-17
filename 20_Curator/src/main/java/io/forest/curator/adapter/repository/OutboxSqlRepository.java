package io.forest.curator.adapter.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.forest.curator.adapter.repository.jpa.OutboxJpaRepository;
import io.forest.curator.adapter.repository.jpa.model.Outbox;
import io.forest.curator.common.model.Status;
import io.forest.curator.common.repository.NullArgumentRepositoryException;
import io.forest.curator.port.OutboxRepository;
import io.forest.curator.port.dto.OutboxDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class OutboxSqlRepository implements OutboxRepository {

	@NonNull
	OutboxJpaRepository repository;

	@Override
	public OutboxDto create(OutboxDto taskDto) {
		return Optional.ofNullable(taskDto)
				.map(toEntity)
				.map(repository::save)
				.map(toDTO)
				.orElseThrow(() -> new NullArgumentRepositoryException());
	}

	@Override
	public List<OutboxDto> findByStatus(Status taskStatus) {
		List<OutboxDto> result = this.repository.findByStatus(taskStatus)
				.stream()
				.map(toDTO)
				.collect(Collectors.toList());

		log.info("Find by status result [size={}]", result.size());
		return result;
	}

	@Override
	public Optional<OutboxDto> save(OutboxDto taskDTO) {
		log.info("Saving taskDTO [dto={}]", taskDTO);

		return Optional.ofNullable(taskDTO)
				.map(toEntity)
				.map(repository::save)
				.map(toDTO);
	}

	Function<OutboxDto, Outbox> toEntity = dto -> new Outbox().setId(dto.getId())
			.setMetadata(dto.getMetadata())
			.setOperation(dto.getOperation())
			.setPayload(dto.getPayload())
			.setStatus(dto.getStatus());

	Function<Outbox, OutboxDto> toDTO = entity -> new OutboxDto().setId(entity.getId())
			.setMetadata(entity.getMetadata())
			.setOperation(entity.getOperation())
			.setPayload(entity.getPayload())
			.setStatus(entity.getStatus());

}
