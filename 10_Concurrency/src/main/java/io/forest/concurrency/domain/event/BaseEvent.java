package io.forest.concurrency.domain.event;

import java.time.ZonedDateTime;
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
public abstract class BaseEvent {

	UUID eventId;

	ZonedDateTime eventTime;
}
