package io.forest.hibernate.conf;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.forest.hibernate.application.CreateMessageApplication;
import io.forest.hibernate.port.dto.NotificationDto;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class MessageAPIAdapter {

	@Autowired
	CreateMessageApplication application;

	@Bean
	RouterFunction<ServerResponse> composedNotifyRoutes() {
		return route().POST("/notify", accept(MediaType.APPLICATION_JSON), this::createNotify)
				.build();
	}

	Mono<ServerResponse> createNotify(ServerRequest request) {
		log.info("Request received");

		return request.bodyToMono(NotificationDto.class)
				.doOnNext(n -> log.info("Payload={}", n))
				.flatMap(application::handleIncomingMessage)
				.flatMap(d -> ok().bodyValue(d.getMessage()));
	}
}
