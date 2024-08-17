package io.forest.curator.port.dto;

import java.util.UUID;

import io.forest.curator.common.model.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class OutboxDto {

	UUID id;

	String metadata;

	String operation;

	String payload;

	Status status;
}
