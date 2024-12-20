package io.forest.redis.adapter.repository.keyvalue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
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
public class UserRepositoryKeyValueAdapter implements UserRepository {

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

		return redisTemplate.opsForValue()
				.multiSet(map)
				.filter(it -> true == it)
				.map(it -> user)
				.switchIfEmpty(Mono.error(() -> new RuntimeException("Unable to create user")));
	}

	@Override
	public Mono<User> findById(UUID id) {

		log.info("Finding user by id: " + id);

		String idStr = id.toString();

		 UserBuilder userBuilder = new UserBuilder();

		List<String> keys = List.of("user:%s:firstName", "user:%s:lastName", "user:%s:email")
				.stream()
				.map(it -> String.format(it, idStr))
				.toList();

		List<Consumer<String>> bindFields = List.of((firstName) -> userBuilder.firstName(firstName),
				(lastName) -> userBuilder.lastName(lastName),
				(email) -> userBuilder.email(email));

		redisTemplate.opsForValue()
				.multiGet(keys)
				.doOnNext(it -> log.info("values >>>> {}", it))
				.flatMapIterable(Function.identity())
				.cast(String.class)
				.doOnNext(it -> log.info(">>>> {}", it))
				.zipWithIterable(bindFields,
						(	value,
							func) -> {
							log.info("apply >>>> {} {}", value, func);
							func.accept(value);
							return value;
						})
				.subscribe();

		User user = userBuilder.build();

		return Mono.just(user);

//		
//
//		redisTemplate.scan(ScanOptions.scanOptions()
//				.match(keyPattern)
//				.count(10000)
//				.build())
//				.doOnNext(it -> log.info("it >>>>> " + it))
//				.map(it -> {
//					String[] split = it.split(":");
//					
//					log.info("Splitted string>>>> " + split);
//
//					switch (split[2]) {
//					case "firstName":
//						
//						
//						userBuilder.firstName(split[2]);
//						break;
//					case "lastName":
//						userBuilder.lastName(split[2]);
//						break;
//					case "email":
//						userBuilder.email(split[2]);
//						break;
//
//					}
//					return it;
//				})
//				.switchIfEmpty(Mono.empty())
//				.subscribe();
//
//		return Mono.just(userBuilder.build());
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
