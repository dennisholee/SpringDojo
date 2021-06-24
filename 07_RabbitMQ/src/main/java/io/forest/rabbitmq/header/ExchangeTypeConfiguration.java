package io.forest.rabbitmq.header;

import org.springframework.amqp.core.HeadersExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("headers")
public class ExchangeTypeConfiguration {

	@Value("${rabbitmq.exchange.type}")
	String exchangeType;

	@Bean
	HeadersExchange exchange() {
		System.out.println("Exchange type: " + exchangeType);
		return new HeadersExchange(exchangeType);
	}
}
