package io.forest.rabbitmq.header;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("headers")
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
		System.out.println("Sending header message...");
		rabbitTemplate.convertAndSend(exchangeType, "", "Hello FOO header from RabbitMQ!", m -> {
			m.getMessageProperties().getHeaders().put("key", "foo");
			return m;

		});
		rabbitTemplate.convertAndSend(exchangeType, "", "Hello BAZ header from RabbitMQ!", m -> {
			m.getMessageProperties().getHeaders().put("key", "baz");
			return m;

		});
		
		rabbitTemplate.convertAndSend(exchangeType, "", "Hello UNKNOWN header from RabbitMQ!", m -> {
			m.getMessageProperties().getHeaders().put("key", "unknown");
			return m;

		});
	}

}
