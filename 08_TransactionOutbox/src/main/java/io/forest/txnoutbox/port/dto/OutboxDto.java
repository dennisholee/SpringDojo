package io.forest.txnoutbox.port.dto;

import java.util.Map;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@ToString
public class OutboxDto {

	UUID id;

	Map<String, String> metadata;

	String operation;

	String payload;
}
