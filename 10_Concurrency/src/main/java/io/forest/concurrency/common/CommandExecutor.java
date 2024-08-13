package io.forest.concurrency.common;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@EnableAsync
@Log4j2
public class CommandExecutor<C extends Command<?>> {

	@NonNull
	CommandQueue<C> commandQueue;

	@Async("foo")
	public void consume() {
		log.info("Consuming command.");

		C command = this.commandQueue.pop();
		command.execute();
	}

}
