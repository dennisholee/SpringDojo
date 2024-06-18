package io.forest.txnoutbox.application;

import java.util.UUID;
import java.util.function.Function;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.forest.txnoutbox.port.dto.ClaimCreatedEvent;
import io.forest.txnoutbox.port.dto.ClaimDto;
import io.forest.txnoutbox.port.dto.OutboxDto;
import io.forest.txnoutbox.port.in.CreateClaim;
import io.forest.txnoutbox.port.out.ClaimRepository;
import io.forest.txnoutbox.port.out.OutboxRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class CreateClaimApplication implements CreateClaim {

	@NonNull
	ClaimRepository claimRepository;

	@NonNull
	OutboxRepository outboxRepository;

	@Override
	public Mono<ClaimDto> handleRequest(ClaimDto claimDto) {
		return Mono.just(claimDto)
				.flatMap(claimRepository::save);
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handleEvent(ClaimCreatedEvent event) {
		Mono.just(event)
				.map(eventToOutboxDto)
				.flatMap(outboxRepository::save)
				.doOnNext(o -> log.info("Outbox created={}", o))
				.subscribe();
	}

	Function<ClaimCreatedEvent, OutboxDto> eventToOutboxDto = event -> {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		String payload = "error";
		
		ClaimDto claimDto = event.getClaimDto();
		
		try {
			payload = objectMapper.writeValueAsString(claimDto);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		OutboxDto outboxDto = new OutboxDto();
		outboxDto.setId(UUID.randomUUID());
		outboxDto.setOperation("new");
		outboxDto.setPayload(payload);

		return outboxDto;
	};

}
