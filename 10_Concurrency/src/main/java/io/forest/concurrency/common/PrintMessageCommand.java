package io.forest.concurrency.common;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PrintMessageCommand implements Command<Void> {

	@NonNull
	String message;

	@Override
	public Void execute() {

		System.out.println("PrintMessageCommand> " + message);

		return null;
	}
}
