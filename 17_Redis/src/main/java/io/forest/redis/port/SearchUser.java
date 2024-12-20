package io.forest.redis.port;

import io.forest.redis.port.command.GetUserCommand;
import io.forest.redis.port.command.SearchUserCommand;
import io.forest.redis.port.dto.UserDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SearchUser {

	Flux<UserDTO> handleRequest(SearchUserCommand command);

	Mono<UserDTO> handleRequest(GetUserCommand command);

}
