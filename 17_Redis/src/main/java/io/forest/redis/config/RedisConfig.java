package io.forest.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.forest.redis.adapter.repository.model.UserEntity;

@EnableRedisRepositories
public class RedisConfig {

	@Bean
	ReactiveRedisTemplate<String, UserEntity> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

		JdkSerializationRedisSerializer jdkSerializationRedisSerializer = new JdkSerializationRedisSerializer();
		StringRedisSerializer stringRedisSerializer = StringRedisSerializer.UTF_8;

//		GenericToStringSerializer<UserEntity> userEntityToStringSerializer = new GenericToStringSerializer<>(UserEntity.class);
		Jackson2JsonRedisSerializer<UserEntity> valueSerializer = new Jackson2JsonRedisSerializer<>(UserEntity.class);

		ReactiveRedisTemplate<String, UserEntity> template = new ReactiveRedisTemplate<>(
				factory,
				RedisSerializationContext.<String, UserEntity>newSerializationContext(jdkSerializationRedisSerializer)
						.key(stringRedisSerializer)
						.value(valueSerializer)
						.build());
		return template;
	}
	
	
	@Bean("objectRedisTemplate")
	ReactiveRedisTemplate<String, Object> objectRedisTemplate(ReactiveRedisConnectionFactory factory) {

		JdkSerializationRedisSerializer jdkSerializationRedisSerializer = new JdkSerializationRedisSerializer();
		StringRedisSerializer stringRedisSerializer = StringRedisSerializer.UTF_8;
		

		GenericToStringSerializer<Object> objectToStrngSerializer = new GenericToStringSerializer<>(Object.class);
//		Jackson2JsonRedisSerializer<UserEntity> valueSerializer = new Jackson2JsonRedisSerializer<>(UserEntity.class);

		ReactiveRedisTemplate<String, Object> template = new ReactiveRedisTemplate<>(
				factory,
				RedisSerializationContext.<String, Object>newSerializationContext(jdkSerializationRedisSerializer)
						.key(stringRedisSerializer)
						.value(objectToStrngSerializer)
						.build());
		return template;
	}
}
