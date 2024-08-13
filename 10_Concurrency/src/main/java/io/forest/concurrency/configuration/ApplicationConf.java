package io.forest.concurrency.configuration;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.forest.concurrency.application.ConsentMapper;
import io.forest.concurrency.application.ConsentMapperImpl;
import io.forest.concurrency.application.FooApplication;
import io.forest.concurrency.application.UpdateAlphaApplication;
import io.forest.concurrency.application.UpdateBetaApplication;
import io.forest.concurrency.application.UpdateConsentFlagApplication;
import io.forest.concurrency.common.Command;
import io.forest.concurrency.common.CommandExecutor;
import io.forest.concurrency.common.CommandQueue;
import io.forest.concurrency.common.CommandQueueDecorator;
import io.forest.concurrency.domain.model.ConsentBuilder;
import io.forest.concurrency.port.ConsentsRepository;

public class ApplicationConf {

	@Bean
	Queue<Command> blockingQueue() {
		return new ConcurrentLinkedQueue<Command>();
	}

	@Bean
	CommandQueue commandQueue(@Autowired Queue<Command> blockingQueue) {
		return new CommandQueueDecorator(blockingQueue);
	}

	@Bean
	FooApplication fooApplication(	@Autowired CommandQueue commandQueue,
									@Autowired CommandExecutor commandPoolExecutor) {
		return new FooApplication(commandQueue, commandPoolExecutor);
	}

	@Bean
	CommandExecutor commandPoolExecutor(@Autowired CommandQueue commandQueue) {
		return new CommandExecutor(commandQueue);
	}

	@Bean(name = "foo")
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5); // Set the core pool size
		executor.setMaxPoolSize(10); // Set the maximum pool size
		executor.setThreadNamePrefix("Async-"); // Set the thread name prefix

		executor.initialize();
		return executor;
	}

	@Bean(name = "Application.consentMapper")
	ConsentMapper mapper() {
		return new ConsentMapperImpl();
	}

	@Bean
	UpdateConsentFlagApplication updateConsentFlagApplication(	@Autowired ConsentsRepository repository,
																@Autowired @Qualifier("Application.consentMapper") ConsentMapper mapper,
																@Autowired ConsentBuilder consentBuilder) {
		return new UpdateConsentFlagApplication(repository, mapper, consentBuilder);
	}
	
	@Bean
	UpdateAlphaApplication updateAlphaApplication() {
		return new UpdateAlphaApplication();
	}
	
	@Bean
	UpdateBetaApplication updateBetaApplication() {
		return new UpdateBetaApplication();
	}
}
