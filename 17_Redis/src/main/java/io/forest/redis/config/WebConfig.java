package io.forest.redis.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.forest.redis.port.RegisterUser;
import io.forest.redis.port.SearchUser;
import io.forest.redis.port.command.GetUserCommand;
import io.forest.redis.port.command.RegisterUserCommand;
import io.forest.redis.port.command.SearchUserCommand;
import io.forest.redis.port.dto.UserDTO;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class WebConfig {

	@Autowired
	RegisterUser registerUser;

	@Autowired
	SearchUser searchUser;

	@Autowired

	@Bean
	public RouterFunction<ServerResponse> routes() {
		return RouterFunctions.nest(RequestPredicates.path("/api"),
				RouterFunctions.route()
						.POST("/users", this::handleUserRegister)
						.POST("/users/search", this::handleSearch)
						.GET("/users/{id}", this::handleGetUser)
						.build());
	}

	Mono<ServerResponse> handleUserRegister(ServerRequest request) {
		return request.bodyToMono(RegisterUserCommand.class)
				.doOnNext(it -> log.info("Received user registration command [command={}]", it))
				.flatMap(registerUser::handleRequest)
				.flatMap(it -> ServerResponse.ok()
						.body(Mono.just(it), UserDTO.class))
				.switchIfEmpty(ServerResponse.badRequest()
						.build());
	}

	Mono<ServerResponse> handleSearch(ServerRequest request) {
		return request.bodyToMono(SearchUserCommand.class)
				.flatMapMany(searchUser::handleRequest)
				.collectList()
				.flatMap(it -> ServerResponse.ok()
						.bodyValue(it));
	}

	Mono<ServerResponse> handleGetUser(ServerRequest request) {

		String id = request.pathVariable("id");

		return Mono.just(id)
				.map(it -> {
					GetUserCommand cmd = new GetUserCommand();
					cmd.setUserId(UUID.fromString(it));
					return cmd;
				})
				.flatMap(searchUser::handleRequest)
				.flatMap(it -> ServerResponse.ok()
						.body(Mono.just(it), UserDTO.class))
				.switchIfEmpty(ServerResponse.notFound()
						.build());
	}
}
