package io.forest.concurrency.adapter.repository;

import java.util.Optional;
import java.util.UUID;

import io.forest.concurrency.adapter.repository.h2.ConsentH2Repository;
import io.forest.concurrency.port.ConsentsRepository;
import io.forest.concurrency.port.dto.ConsentDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsentRepositoryAdapter implements ConsentsRepository {

	@NonNull
	ConsentH2Repository repository;

	@NonNull
	ConsentMapper mapper;

	@Override
	public Optional<ConsentDTO> findById(UUID id) {
		return this.repository.findById(id)
				.map(mapper::map);
	}

}
