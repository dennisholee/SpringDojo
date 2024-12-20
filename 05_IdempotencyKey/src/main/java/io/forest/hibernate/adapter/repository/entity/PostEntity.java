package io.forest.hibernate.adapter.repository.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Table("post")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PostEntity {

	@Id
	@Column("post_id")
	UUID id;

	@Column("author")
	String author;

	@Column("message")
	String message;
	
	@Version
	@Column("version")
	long version;
}
