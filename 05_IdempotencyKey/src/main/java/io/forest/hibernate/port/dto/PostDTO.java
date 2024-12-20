package io.forest.hibernate.port.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class PostDTO {
	UUID id;

	String author;

	String message;

	long version;
}
