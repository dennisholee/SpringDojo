package io.forest.redis.port;

import java.util.UUID;

import io.forest.redis.domain.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository {

	Mono<User> create(User user);

	Mono<User> findById(UUID id);

	Flux<User> findByName(	String firstName,
							String lastName);
}
