package io.forest.txnoutbox.conf;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.forest.txnoutbox.port.dto.ClaimDto;
import io.forest.txnoutbox.port.in.CreateClaim;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class AdapterRestApiConf {

	@Autowired
	CreateClaim createClaim;

	@Bean
	RouterFunction<ServerResponse> composedNotifyRoutes() {
		return route().POST("/claims", accept(MediaType.APPLICATION_JSON), this::createClaim)
				.build();
	}

	Mono<ServerResponse> createClaim(ServerRequest request) {
		log.info("Request received");

		return request.bodyToMono(ClaimDto.class)
				.doOnNext(n -> log.info("Payload={}", n))
				.flatMap(createClaim::handleRequest)
				.doOnNext(n -> log.info("Claim created={}", n))
				.flatMap(c -> ServerResponse.ok()
						.bodyValue(c));
	}

}
