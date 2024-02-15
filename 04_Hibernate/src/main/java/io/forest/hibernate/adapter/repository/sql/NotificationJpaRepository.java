package io.forest.hibernate.adapter.repository.sql;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import io.forest.hibernate.adapter.repository.sql.model.Notification;
import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

public interface NotificationJpaRepository extends JpaRepository<Notification, String> {

	@Override
	@Transactional(value = TxType.REQUIRED)
	Notification save(Notification notification);

	@Override
	@QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true") })
	List<Notification> findAll();
}
