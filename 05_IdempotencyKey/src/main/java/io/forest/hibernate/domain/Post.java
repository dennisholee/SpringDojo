package io.forest.hibernate.domain;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Post {

	UUID id;

	String author;

	String message;

	long version;

	public void create() {
		this.id = UUID.randomUUID();
	}
}
