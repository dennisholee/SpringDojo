package io.forest.concurrency.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.forest.concurrency.common.events.DomainEventPublisher;
import io.forest.concurrency.common.events.DomainEventSpringPublisher;
import io.forest.concurrency.domain.model.ConsentBuilder;

public class DomainConf {

	@Bean
	DomainEventPublisher domainEventPublisher(@Autowired ApplicationEventPublisher applicationEventPublisher) {
		return new DomainEventSpringPublisher(applicationEventPublisher);
	}

	@Bean(name = "DomainEvent.taskExecutor")
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5); // Set the core pool size
		executor.setMaxPoolSize(10); // Set the maximum pool size
		executor.setThreadNamePrefix("EventTaskExec-"); // Set the thread name prefix
		executor.initialize();
		return executor;
	}

	@Bean(name = "applicationEventMulticaster")
	public ApplicationEventMulticaster applicationEventMulticaster(@Autowired @Qualifier("DomainEvent.taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
		SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
		eventMulticaster.setTaskExecutor(taskExecutor);
		return eventMulticaster;
	}

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	ConsentBuilder consentBuilder(@Autowired DomainEventPublisher domainEventPublisher) {
		return new ConsentBuilder(domainEventPublisher);
	}
}
