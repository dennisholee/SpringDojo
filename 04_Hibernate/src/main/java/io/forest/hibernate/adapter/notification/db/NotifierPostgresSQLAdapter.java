package io.forest.hibernate.adapter.notification.db;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

import io.forest.hibernate.common.ResponseDto;
import io.forest.hibernate.common.ResponseStatus;
import io.forest.hibernate.port.dto.NotificationDto;
import io.forest.hibernate.port.out.NotifierAdapter;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
public class NotifierPostgresSQLAdapter implements NotifierAdapter {

	String channel;

	JdbcTemplate jdbcTemplate;

	@Override
	public ResponseDto<NotificationDto> notify(NotificationDto t) {
		log.info("Notify message={}",
				Optional.of(t)
						.map(NotificationDto::toString)
						.orElse(""));

		jdbcTemplate.execute(String.format("NOTIFY %s, '%s'", channel, t));

		return ResponseDto.<NotificationDto>builder()
				.responseStatus(ResponseStatus.OK)
				.data(t)
				.build();
	}

}
