package io.forest.redis.app;

import static io.forest.redis.app.UserMapper.toDTO;
import static io.forest.redis.app.UserMapper.toDomain;

import io.forest.redis.domain.User;
import io.forest.redis.port.RegisterUser;
import io.forest.redis.port.UserRepository;
import io.forest.redis.port.command.RegisterUserCommand;
import io.forest.redis.port.dto.UserDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class RegisterUserApp implements RegisterUser {

	@NonNull
	UserRepository userRepository;

	@Override
	public Mono<UserDTO> handleRequest(RegisterUserCommand command) {

		return Mono.just(command)
				.map(toDomain)
				.doOnNext(it -> log.info("Proceeding to register user [user={}]", it))
				.map(User::register)
				.doOnNext(it -> log.info("Proceeding to persist user [user={}]", it))
				.flatMap(userRepository::create)
				.doOnNext(it -> log.info("User registration process completed [user={}]", it))
				.map(toDTO);
	}

	
}
