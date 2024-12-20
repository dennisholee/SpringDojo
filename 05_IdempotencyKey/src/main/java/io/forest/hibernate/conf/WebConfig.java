package io.forest.hibernate.conf;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.forest.hibernate.application.command.SavePostCommand;
import io.forest.hibernate.common.idempotency.IdempotencyFilter;
import io.forest.hibernate.common.idempotency.repository.RequestLogRepository;
import io.forest.hibernate.common.idempotency.response.StateResponseFactory;
import io.forest.hibernate.port.SavePost;
import io.forest.hibernate.port.dto.PostDTO;
import reactor.core.publisher.Mono;

public class WebConfig {

	@Autowired
	SavePost savePost;

	@Bean
	public RouterFunction<ServerResponse> routes(	SavePost savePost,
													StateResponseFactory requestStateResponseFactory,
													RequestLogRepository requestLogRepository) {
		return route().POST("/", this::savePost)
				.filter(new IdempotencyFilter(requestStateResponseFactory, requestLogRepository))
				.build();
	}

	Mono<ServerResponse> savePost(ServerRequest serverRequest) {
		return serverRequest.bodyToMono(SavePostCommand.class)
				.flatMap(savePost::handleRequest)
				.flatMap(dto -> ok().body(Mono.just(dto), PostDTO.class));
	}
}
