package io.forest.concurrency.configuration;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.forest.concurrency.application.FooApplication;
import io.forest.concurrency.application.UpdateConsentFlagApplication;
import io.forest.concurrency.port.UpdateConsentCommand;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class AdapterRestApiConf {

	@Autowired(required = true)
	FooApplication fooApplication;

	@Autowired(required = true)
	UpdateConsentFlagApplication updateConsentFlagApplication;

	@Bean
	RouterFunction<ServerResponse> composedNotifyRoutes() {
		return route().GET("/foo", accept(MediaType.APPLICATION_JSON), this::foo)
				.POST("/consent", accept(MediaType.APPLICATION_JSON), this::consent)
				.build();
	}

	Mono<ServerResponse> foo(ServerRequest request) {
		log.info("Request received");

		fooApplication.foo();

		return ServerResponse.ok()
				.bodyValue("OK");
	}

	Mono<ServerResponse> consent(ServerRequest request) {
		log.info("Request received");

		return request.bodyToMono(UpdateConsentCommand.class)
				.doOnNext(it -> log.info("Update consent command [payload={}]", it))
				.doOnNext(updateConsentFlagApplication::handleRequest)
				.flatMap(it -> ServerResponse.ok()
						.build());
	}

}
