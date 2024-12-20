package io.forest.redis.adapter.repository.model;

import java.util.UUID;

import org.springframework.data.redis.core.RedisHash;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@RedisHash("")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class UserEntity {

	UUID id;

	String firstName;

	String lastName;
	
	String email;
}
