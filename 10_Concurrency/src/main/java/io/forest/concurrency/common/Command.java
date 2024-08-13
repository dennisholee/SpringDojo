package io.forest.concurrency.common;

public interface Command<R> {

	R execute();
}
