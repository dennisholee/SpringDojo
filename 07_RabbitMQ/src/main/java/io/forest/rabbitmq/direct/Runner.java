package io.forest.rabbitmq.direct;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("direct")
@Component
public class Runner implements CommandLineRunner {

	@Value("${rabbitmq.exchange.type}")
	String exchangeType;

	private final RabbitTemplate rabbitTemplate;

	public Runner(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Sending direct message...");
		rabbitTemplate.convertAndSend(exchangeType, "foo", "Hello FOO Direct from RabbitMQ!");
		rabbitTemplate.convertAndSend(exchangeType, "baz", "Hello BAZ Direct from RabbitMQ!");
		
		rabbitTemplate.convertAndSend(exchangeType, "baz.baz", "Hello BAZ Direct from RabbitMQ!");
	}

}
