package io.forest.hibernate.port;

import io.forest.hibernate.application.command.SavePostCommand;
import io.forest.hibernate.port.dto.PostDTO;
import reactor.core.publisher.Mono;

public interface SavePost {

	Mono<PostDTO> handleRequest(SavePostCommand savePostCommand);

}
