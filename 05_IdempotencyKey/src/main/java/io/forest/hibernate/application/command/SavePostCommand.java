package io.forest.hibernate.application.command;

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
public class SavePostCommand {

	String requestId;
	
	String author;
	
	String message;
	
}
