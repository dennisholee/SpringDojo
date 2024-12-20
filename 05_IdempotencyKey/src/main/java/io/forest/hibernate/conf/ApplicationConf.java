package io.forest.hibernate.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.forest.hibernate.application.CreateMessageApplication;
import io.forest.hibernate.application.FindMessageApplication;
import io.forest.hibernate.port.out.NotificationRepository;
import io.forest.hibernate.port.out.NotifierAdapter;
import lombok.NonNull;

public class ApplicationConf {

	@Bean
	CreateMessageApplication createMessageApplication(	@Autowired NotifierAdapter notifierAdapter,
														@Autowired NotificationRepository notificationRepository) {
		return new CreateMessageApplication(notifierAdapter, notificationRepository);
	}

	@Bean
	FindMessageApplication findMessageApplication(@Autowired NotificationRepository notificationRepository) {
		return new FindMessageApplication(notificationRepository);
	}
}
