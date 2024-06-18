package io.forest.txnoutbox.adapter.repository.sql;

import static jakarta.transaction.Transactional.TxType.REQUIRED;

import java.util.function.Function;

import io.forest.txnoutbox.adapter.repository.sql.model.Outbox;
import io.forest.txnoutbox.port.dto.OutboxDto;
import io.forest.txnoutbox.port.out.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
@RequiredArgsConstructor
public class OutboxSqlRepository implements OutboxRepository {

	@NonNull
	OutboxJpaRepository jpaRepository;

	@Transactional(value = REQUIRED)
	@Override
	public Mono<OutboxDto> save(OutboxDto dto) {
		return Mono.just(dto)
				.doOnNext(d -> log.info("Saving outbox={}", d))
				.map(dtoToEntity)
				.map(this.jpaRepository::save)
				.map(entityToDto);

	}

	Function<OutboxDto, Outbox> dtoToEntity = dto -> {
		Outbox outbox = new Outbox();
		outbox.setId(dto.getId());
		outbox.setMetadata("{}");
		outbox.setOperation(dto.getOperation());
		outbox.setPayload(dto.getPayload());
		return outbox;
	};

	Function<Outbox, OutboxDto> entityToDto = entity -> {
		OutboxDto dto = new OutboxDto();
		dto.setId(entity.getId());
		dto.setMetadata(null);
		dto.setOperation(entity.getOperation());
		dto.setPayload(entity.getPayload());
		return dto;
	};

}
