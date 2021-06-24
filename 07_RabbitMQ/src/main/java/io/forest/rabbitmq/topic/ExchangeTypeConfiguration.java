package io.forest.rabbitmq.topic;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("topic")
public class ExchangeTypeConfiguration {
	
	@Value("${rabbitmq.exchange.type}")
	String exchangeType;

	@Bean
	TopicExchange exchange() {
		System.out.println("Exchange type: " + exchangeType);
		return new TopicExchange(exchangeType);
	}
}
