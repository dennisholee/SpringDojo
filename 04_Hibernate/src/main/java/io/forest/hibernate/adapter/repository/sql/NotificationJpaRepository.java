package io.forest.hibernate.adapter.repository.sql;

import org.springframework.data.jpa.repository.JpaRepository;

import io.forest.hibernate.adapter.repository.sql.model.Notification;

public interface NotificationJpaRepository extends JpaRepository<Notification, String> {

}
