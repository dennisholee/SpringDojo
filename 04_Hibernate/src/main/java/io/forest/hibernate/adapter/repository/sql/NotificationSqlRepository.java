package io.forest.hibernate.adapter.repository.sql;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.jpa.repository.QueryHints;

import io.forest.hibernate.adapter.repository.sql.model.Notification;
import io.forest.hibernate.port.dto.NotificationDto;
import io.forest.hibernate.port.out.NotificationRepository;
import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class NotificationSqlRepository implements NotificationRepository {

	@NonNull
	NotificationJpaRepository notificationJpaRepository;

	@Override
	@Transactional(value = TxType.REQUIRED)
	public Mono<NotificationDto> save(NotificationDto dto) {
		return Mono.just(dto)
				.map(dtoToEntity)
				.map(this.notificationJpaRepository::save)
				.map(entityToDto);
	}

	public Flux<NotificationDto> findAll() {
		return Flux.fromStream(this.notificationJpaRepository.findAll()
				.stream())
				.map(entityToDto);
	}

	@Override
	public Mono<NotificationDto> findById(String id) {
		Optional<Notification> notification = notificationJpaRepository.findById(id);
		return notification.map(entityToDto)
				.map(Mono::just)
				.orElse(Mono.empty());
	}

	Function<NotificationDto, Notification> dtoToEntity = dto -> {
		Notification n = new Notification();
		n.setId(dto.getId());
		n.setMessage(dto.getMessage());
		n.setCreateDateTime(dto.getCreateDateTime());
		n.setModifyDateTime(dto.getModifyDateTime());
		return n;
	};

	Function<Notification, NotificationDto> entityToDto = entity -> Optional.ofNullable(entity)
			.map(e -> {
				NotificationDto dto = new NotificationDto();
				dto.setId(e.getId());
				dto.setMessage(e.getMessage());
				dto.setCreateDateTime(entity.getCreateDateTime());
				dto.setModifyDateTime(entity.getModifyDateTime());

				return dto;
			})
			.orElseThrow();

}
