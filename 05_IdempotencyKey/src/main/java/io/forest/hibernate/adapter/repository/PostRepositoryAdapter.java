package io.forest.hibernate.adapter.repository;

import java.util.function.Function;

import io.forest.hibernate.adapter.repository.entity.PostEntity;
import io.forest.hibernate.adapter.repository.h2.PostH2Repository;
import io.forest.hibernate.domain.Post;
import io.forest.hibernate.port.PostRepository;
import io.forest.hibernate.port.dto.PostDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class PostRepositoryAdapter implements PostRepository {

	@NonNull
	PostH2Repository repository;

	@Override
	public Mono<PostDTO> create(Post post) {
		return Mono.just(post)
				.map(toEntity)
				.flatMap(repository::save)
				.map(toDTO);
	}

	Function<Post, PostEntity> toEntity = p -> new PostEntity().setAuthor(p.getAuthor())
			.setId(p.getId())
			.setMessage(p.getMessage());

	Function<PostEntity, PostDTO> toDTO = p -> PostDTO.builder()
			.author(p.getAuthor())
			.id(p.getId())
			.message(p.getMessage())
			.version(p.getVersion())
			.build();

}
