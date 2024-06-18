package io.forest.txnoutbox.port.out;

import io.forest.txnoutbox.port.dto.ClaimDto;
import reactor.core.publisher.Mono;

public interface ClaimRepository {

	Mono<ClaimDto> save(ClaimDto claimDto);

}
