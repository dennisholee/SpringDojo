package io.forest.txnoutbox.adapter.repository.sql;

import static jakarta.transaction.Transactional.TxType.REQUIRED;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;

import io.forest.txnoutbox.adapter.repository.sql.model.Claim;
import io.forest.txnoutbox.port.dto.ClaimDto;
import io.forest.txnoutbox.port.out.ClaimRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ClaimSqlRepository implements ClaimRepository {

	@NonNull
	ClaimJpaRepository claimJpaRepository;

	@Override
	@Transactional(value = REQUIRED)
	public Mono<ClaimDto> save(ClaimDto claimDto) {

		return Mono.just(claimDto)
				.map(dtoToEntity)
				.map(claimJpaRepository::save)
				.map(entityToDto);
	}

	Function<ClaimDto, Claim> dtoToEntity = dto -> {
		Claim c = new Claim();
		c.setId(UUID.randomUUID());
		c.setNote(dto.getNote());
		c.setSubmissionDate(LocalDate.now());
		return c;
	};

	Function<Claim, ClaimDto> entityToDto = entity -> {
		ClaimDto dto = new ClaimDto();
		dto.setId(entity.getId());
		dto.setNote(entity.getNote());
		dto.setSubmissionDate(entity.getSubmissionDate());
		return dto;
	};
}
