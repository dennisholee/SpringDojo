package io.forest.redis.adapter.repository.keyvalue;

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import io.forest.redis.domain.User;
import io.forest.redis.domain.UserBuilder;

@ReadingConverter
public class UserReader implements Converter<Map<String, String>, User> {

	@Override
	public User convert(Map<String, String> source) {

		return new UserBuilder().id(source.get("id"))
				.firstName(source.get("firstName"))
				.lastName(source.get("lastName"))
				.email(source.get("email"))
				.build();
	}
}