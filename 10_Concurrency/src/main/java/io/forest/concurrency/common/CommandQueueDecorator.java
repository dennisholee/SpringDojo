package io.forest.concurrency.common;

import java.util.Queue;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class CommandQueueDecorator<C extends Command<?>> implements CommandQueue<C> {

	@NonNull
	Queue<C> queue;

	@Override
	public boolean put(C command) {

		try {
			queue.add(command);

			log.info("Worker queue size is {}", this.queue.size());
			return true;
		} catch (Exception e) {
			log.warn(e);
			return false;
		}
	}

	@Override
	public C pop() {

		C command = this.queue.poll();
		log.info("Dequeue command [cmd={}]", command);

		return command;
	}

}
