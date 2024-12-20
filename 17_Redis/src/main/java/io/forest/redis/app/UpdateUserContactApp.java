package io.forest.redis.app;

import io.forest.redis.port.UserRepository;
import io.forest.redis.port.command.UpdateUserContactCommand;
import io.forest.redis.port.dto.UserDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UpdateUserContactApp {

	@NonNull
	UserRepository userRepository;
	
	public UserDTO handleRequest(UpdateUserContactCommand command) {
		
		Mono.just(command).map(UpdateUserContactCommand::getId).map(userRepository::findById);
		
		return null;
	}
}
