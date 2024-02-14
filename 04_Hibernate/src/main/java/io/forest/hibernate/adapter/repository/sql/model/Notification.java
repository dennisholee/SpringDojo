package io.forest.hibernate.adapter.repository.sql.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@ToString
@NoArgsConstructor
@Data
@Table(name = "NOTIFICATIONS")
public class Notification {

	@Id
	String id;

	LocalDateTime createDateTime;

	LocalDateTime modifyDateTime;

	String message;

}
