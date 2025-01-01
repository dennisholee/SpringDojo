package io.forest.springai.huggingface.port.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class MessageDto {

	String message;
	
	byte[] raw;
}
