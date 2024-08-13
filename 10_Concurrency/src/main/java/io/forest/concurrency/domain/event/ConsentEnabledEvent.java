package io.forest.concurrency.domain.event;

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
public class ConsentEnabledEvent extends BaseEvent {

	boolean consent;
	
}
