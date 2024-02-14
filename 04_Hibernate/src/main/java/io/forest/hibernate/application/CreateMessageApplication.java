package io.forest.hibernate.application;

import java.util.UUID;
import java.util.function.Function;

import io.forest.hibernate.common.ResponseDto;
import io.forest.hibernate.common.ResponseStatus;
import io.forest.hibernate.port.dto.NotificationDto;
import io.forest.hibernate.port.out.NotificationRepository;
import io.forest.hibernate.port.out.NotifierAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
@RequiredArgsConstructor
public class CreateMessageApplication {

	@NonNull
	NotifierAdapter notifierAdapter;

	@NonNull
	NotificationRepository notificationRepository;

	public Mono<NotificationDto> handleIncomingMessage(NotificationDto dto) {
		log.info("Handle incoming message dto={}", dto);

		return Mono.just(dto)
				.doOnNext(d -> d.setId(UUID.randomUUID()
						.toString()))
				.doOnNext(d -> log.info("Converted dto={}", d))
				.doOnNext(notifierAdapter::notify)
				.flatMap(notificationRepository::save);
	}
}
