package io.forest.rabbitmq.fanout;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("fanout")
public class ExchangeTypeConfiguration {

	@Value("${rabbitmq.exchange.type}")
	String exchangeType;

	@Bean
	FanoutExchange exchange() {
		System.out.println("Exchange type: " + exchangeType);
		return new FanoutExchange(exchangeType);
	}
}
