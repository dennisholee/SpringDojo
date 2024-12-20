package io.forest.redis.app;

import static io.forest.redis.app.UserMapper.toDTO;

import io.forest.redis.port.SearchUser;
import io.forest.redis.port.UserRepository;
import io.forest.redis.port.command.GetUserCommand;
import io.forest.redis.port.command.SearchUserCommand;
import io.forest.redis.port.dto.UserDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class SearchUserApp implements SearchUser {

	@NonNull
	UserRepository userRepository;

	@Override
	public Flux<UserDTO> handleRequest(SearchUserCommand command) {
		return userRepository.findByName(command.getFirstName(), command.getLastName())
				.map(toDTO);
	}

	@Override
	public Mono<UserDTO> handleRequest(GetUserCommand command) {
		return userRepository.findById(command.getUserId())
				.map(toDTO);
	}
}
