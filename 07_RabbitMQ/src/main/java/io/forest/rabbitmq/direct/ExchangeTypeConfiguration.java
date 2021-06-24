package io.forest.rabbitmq.direct;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("direct")
public class ExchangeTypeConfiguration {

	@Value("${rabbitmq.exchange.type}")
	String exchangeType;

	@Bean
	DirectExchange exchange() {
		System.out.println("Exchange type: " + exchangeType);
		return new DirectExchange(exchangeType);
	}
}
