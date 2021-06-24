package io.forest.rabbitmq;

import org.springframework.stereotype.Component;

@Component
public class FooReceiver {

	public void receiveMessage(String message) {
		System.out.println("Foo Received>  " + message);
	}
}
