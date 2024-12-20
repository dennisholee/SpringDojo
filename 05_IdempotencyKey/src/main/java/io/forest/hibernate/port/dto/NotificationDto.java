package io.forest.hibernate.port.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@ToString
public class NotificationDto {

	String id;

	LocalDateTime createDateTime;

	LocalDateTime modifyDateTime;

	String message;
}
