package io.forest.redis.port;

import io.forest.redis.port.command.RegisterUserCommand;
import io.forest.redis.port.dto.UserDTO;
import reactor.core.publisher.Mono;

public interface RegisterUser {

	Mono<UserDTO> handleRequest(RegisterUserCommand command);

}
