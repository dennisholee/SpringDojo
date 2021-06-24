package io.forest.rabbitmq;

import org.springframework.stereotype.Component;

@Component
public class BazReceiver {

	public void receiveMessage(String message) {
		System.out.println("Baz Received>  " + message);
	}
}
