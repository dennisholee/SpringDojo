package io.forest.hibernate.application;

import java.util.UUID;
import java.util.function.Function;

import io.forest.hibernate.application.command.SavePostCommand;
import io.forest.hibernate.domain.Post;
import io.forest.hibernate.port.PostRepository;
import io.forest.hibernate.port.SavePost;
import io.forest.hibernate.port.dto.PostDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class SavePostApp implements SavePost {

	@NonNull
	PostRepository postRepository;

	@Override
	public Mono<PostDTO> handleRequest(SavePostCommand savePostCommand) {
		return Mono.just(savePostCommand)
				.map(toPost)
				.doOnNext(Post::create)
				.flatMap(postRepository::create);
	}

	Function<SavePostCommand, Post> toPost = cmd -> Post.builder()
			.author(cmd.getAuthor())
			.message(cmd.getMessage())
			.build();
}
