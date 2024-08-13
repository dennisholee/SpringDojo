package io.forest.concurrency.port;

import java.time.LocalDateTime;
import java.util.UUID;

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
public class UpdateConsentCommand {

	UUID id;

	boolean flag;

	LocalDateTime commandDatetime = LocalDateTime.now();

}
