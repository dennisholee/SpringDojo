package io.forest.hibernate.port;

import io.forest.hibernate.domain.Post;
import io.forest.hibernate.port.dto.PostDTO;
import reactor.core.publisher.Mono;

public interface PostRepository {

	Mono<PostDTO> create(Post post);

}
