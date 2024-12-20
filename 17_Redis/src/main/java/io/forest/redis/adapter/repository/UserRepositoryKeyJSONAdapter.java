package io.forest.redis.adapter.repository;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.data.redis.core.ReactiveRedisTemplate;

import io.forest.redis.adapter.repository.model.UserEntity;
import io.forest.redis.domain.Name;
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
public class UserRepositoryKeyJSONAdapter implements UserRepository {

	@NonNull
	ReactiveRedisTemplate<String, Object> redisTemplate;

	@Override
	public Mono<User> create(User user) {
		UUID id = user.getId();
		Name name = user.getName();

		Map<String, String> map = Map.of(String.format("user:%s:firstName", id, "id"),
				name.getFirstName(),
				String.format("user:%s:lastName", id, "id"),
				name.getLastName(),
				String.format("user:%s:email", id, "id"),
				user.getEmail());

		return redisTemplate.opsForHash()
				.putAll(String.format("user:%s", id), map)
				.filter(it -> true == it)
				.map(it -> user)
				.switchIfEmpty(Mono.error(() -> new RuntimeException("Unable to create user")));
	}

	@Override
	public Mono<User> findById(UUID id) {
		return redisTemplate.opsForList()
				.range("users", 0, -1)
				.cast(UserEntity.class)
				.filter(it -> id.equals(it.getId()))
				.last()
				.cast(UserEntity.class)
				.map(toDomain);
	}

	@Override
	public Flux<User> findByName(	String firstName,
									String lastName) {
		return redisTemplate.opsForList()
				.range("users", 0, -1)
				.cast(UserEntity.class)
				.filter(it -> it.getFirstName()
						.equalsIgnoreCase(firstName)
						&& it.getLastName()
								.equalsIgnoreCase(lastName))
				.cast(UserEntity.class)
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
