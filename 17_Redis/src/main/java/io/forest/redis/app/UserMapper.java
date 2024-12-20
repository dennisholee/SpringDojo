package io.forest.redis.app;

import java.util.function.Function;

import io.forest.redis.domain.User;
import io.forest.redis.domain.UserBuilder;
import io.forest.redis.port.command.RegisterUserCommand;
import io.forest.redis.port.dto.UserDTO;

public class UserMapper {

	static Function<RegisterUserCommand, User> toDomain = cmd -> new UserBuilder()
			.name(cmd.getFirstName(), cmd.getLastName())
			.email(cmd.getEmail())
			.build();

	static Function<User, UserDTO> toDTO = entity -> UserDTO.builder()
			.id(entity.getId())
			.firstName(entity.getName()
					.getFirstName())
			.lastName(entity.getName()
					.getLastName())
			.email(entity.getEmail())
			.build();
}
