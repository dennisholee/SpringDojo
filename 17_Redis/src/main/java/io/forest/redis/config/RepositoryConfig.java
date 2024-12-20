package io.forest.redis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import io.forest.redis.adapter.repository.UserRepositoryKeyValueAdapter;
import io.forest.redis.port.UserRepository;

public class RepositoryConfig {

//	@Bean
//	UserRepository userRepository(ReactiveRedisTemplate<String, UserEntity> template) {
//		return new UserRepositoryAdapter(template);
//	}

	@Bean
	UserRepository userRepositoryKeyValue(@Qualifier("objectRedisTemplate") ReactiveRedisTemplate<String, Object> template) {
//		return new UserRepositoryHashKeyValueAdapter(template);
		return new UserRepositoryKeyValueAdapter(template);
	}

//	@Bean
//	public RedisMappingContext keyValueMappingContext() {
//		return new RedisMappingContext(new MappingConfiguration(new IndexConfiguration() {
//			@Override
//			protected Iterable<IndexDefinition> initialConfiguration() {
//				return Collections.singleton(new SimpleIndexDefinition("people", "firstname"));
//			}
//		}, new KeyspaceConfiguration()));
//	}

}
