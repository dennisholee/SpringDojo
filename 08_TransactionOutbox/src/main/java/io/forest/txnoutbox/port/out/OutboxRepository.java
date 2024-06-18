package io.forest.txnoutbox.port.out;

import io.forest.txnoutbox.port.dto.OutboxDto;
import reactor.core.publisher.Mono;

public interface OutboxRepository {

	Mono<OutboxDto> save(OutboxDto dto);
}
