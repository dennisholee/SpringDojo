package io.forest.curator.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.forest.curator.application.DispatchOutboxApplication;
import io.forest.curator.port.DispatchOutbox;
import io.forest.curator.port.MessageGateway;
import io.forest.curator.port.OutboxRepository;

public class ApplicationConf {

	@Bean
	DispatchOutbox dispatchOutbox(	@Autowired OutboxRepository taskRepository,
									@Autowired MessageGateway messageGateway) {
		return new DispatchOutboxApplication(taskRepository, messageGateway);
	}
}
