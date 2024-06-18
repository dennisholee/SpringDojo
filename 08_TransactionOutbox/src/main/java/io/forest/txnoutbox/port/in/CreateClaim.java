package io.forest.txnoutbox.port.in;

import java.util.Optional;

import io.forest.txnoutbox.port.dto.ClaimCreatedEvent;
import io.forest.txnoutbox.port.dto.ClaimDto;
import io.forest.txnoutbox.port.dto.OutboxDto;
import reactor.core.publisher.Mono;

public interface CreateClaim {

	Mono<ClaimDto> handleRequest(ClaimDto claimDto);

	void handleEvent(ClaimCreatedEvent event);
}
