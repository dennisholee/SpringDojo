package io.forest.redis.adapter.repository;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.data.redis.core.ReactiveRedisTemplate;

import io.forest.redis.adapter.repository.model.UserEntity;
import io.forest.redis.domain.User;
import io.forest.redis.domain.UserBuilder;
import io.forest.redis.port.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class UserRepositoryAdapter implements UserRepository {

	@NonNull
	ReactiveRedisTemplate<String, UserEntity> redisTemplate;

	@Override
	public Mono<User> create(User user) {
		return redisTemplate.opsForList()
				.rightPush("users", toEntity.apply(user))
				.filter(Objects::nonNull)
				.doOnNext(it -> log.info("User persisted to redis [redisKey={}, user={}]", it, user))
				.map(it -> user)
				.switchIfEmpty(Mono.error(() -> new RuntimeException("Unable to create user")));
	}

	@Override
	public Mono<User> findById(UUID id) {
		return redisTemplate.opsForList()
				.range("users", 0, -1)
				.filter(it -> id.equals(it.getId()))
				.last()
				.map(toDomain);
	}

	@Override
	public Flux<User> findByName(	String firstName,
									String lastName) {
		return redisTemplate.opsForList()
				.range("users", 0, -1)
				.filter(it -> it.getFirstName()
						.equalsIgnoreCase(firstName)
						&& it.getLastName()
								.equalsIgnoreCase(lastName))
				.map(toDomain);
	}

	Function<User, UserEntity> toEntity = user -> new UserEntity().setId(user.getId())
			.setFirstName(user.getName()
					.getFirstName())
			.setLastName(user.getName()
					.getLastName())
			.setEmail(user.getEmail());

	Function<UserEntity, User> toDomain = entity -> new UserBuilder().id(entity.getId())
			.name(entity.getFirstName(), entity.getLastName())
			.email(entity.getEmail())
			.build();

}
