package io.forest.concurrency.common;

public interface CommandQueue<C extends Command<?>> {

	boolean put(C command);

	C pop();

}
