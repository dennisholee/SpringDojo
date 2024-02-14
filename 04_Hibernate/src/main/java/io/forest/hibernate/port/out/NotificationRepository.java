package io.forest.hibernate.port.out;

import io.forest.hibernate.port.dto.NotificationDto;
import reactor.core.publisher.Mono;

public interface NotificationRepository {

	Mono<NotificationDto> save(NotificationDto dto);
}
