package io.forest.redis.port.command;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RegisterUserCommand {

	String firstName;
	
	String lastName;
	
	String email;
}
