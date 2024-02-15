package io.forest.hibernate.application;

import io.forest.hibernate.port.dto.NotificationDto;
import io.forest.hibernate.port.out.NotificationRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@RequiredArgsConstructor
public class FindMessageApplication {

	@NonNull
	NotificationRepository notificationRepository;

	public Flux<NotificationDto> handleFindAllMessage() {
		return this.notificationRepository.findAll();
	}
	
	public Mono<NotificationDto> handleFindById(String id) {
		return this.notificationRepository.findById(id);
	}

}
