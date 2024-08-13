package io.forest.concurrency.application;

import io.forest.concurrency.common.Command;
import io.forest.concurrency.common.CommandExecutor;
import io.forest.concurrency.common.CommandQueue;
import io.forest.concurrency.common.PrintMessageCommand;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class FooApplication {

	@NonNull
	CommandQueue<Command<?>> commandQueue;

	@NonNull
	CommandExecutor<Command<?>> commandPoolExecutor;

	public void foo() {

		log.info("Create print message command.");
		Command<Void> cmd = new PrintMessageCommand("Hello World!");

		log.info("Append print message command to queue.");
		commandQueue.put(cmd);

		commandPoolExecutor.consume();
	}

}
