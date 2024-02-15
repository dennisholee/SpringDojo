package io.forest.hibernate.port.out;

import io.forest.hibernate.port.dto.NotificationDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepository {

	Mono<NotificationDto> save(NotificationDto dto);

	Flux<NotificationDto> findAll();

	Mono<NotificationDto> findById(String id);
}
