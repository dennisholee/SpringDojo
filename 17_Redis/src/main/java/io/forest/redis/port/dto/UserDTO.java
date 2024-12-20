package io.forest.redis.port.dto;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
@ToString
@Builder
public class UserDTO {
	
	UUID id;
	
	String firstName;
	
	String lastName;
	
	String email;

}
