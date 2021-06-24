package io.forest.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class BazConfiguration {

	@Value("${rabbitmq.queues.baz}")
	String queueName;

	@Bean("queue_baz")
	Queue queue() {
		return new Queue(queueName, false);
	}

	@Bean("container_baz")
	SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
			@Qualifier("listener_adapter_baz") MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(queueName);
		container.setMessageListener(listenerAdapter);
		return container;
	}

	@Bean("listener_adapter_baz")
	MessageListenerAdapter listenerAdapter(BazReceiver receiver) {
		return new MessageListenerAdapter(receiver, "receiveMessage");
	}

	@Profile("topic")
	private static class TopicConfig {
		@Bean("binding_baz")
		Binding binding(@Qualifier("queue_baz") Queue queue, TopicExchange exchange) {
			System.out.println("BAZ Binding: Topic");
			return BindingBuilder.bind(queue).to(exchange).with("baz.#");
		}
	}

	@Profile("headers")
	private static class HeadersConfig {
		@Bean("binding_baz")
		Binding binding(@Qualifier("queue_baz") Queue queue, HeadersExchange exchange) {
			System.out.println("BAZ Binding: Headers");
			return BindingBuilder.bind(queue).to(exchange).where("key").matches("baz");
		}
	}
	
	@Profile("direct")
	private static class DirectConfig {
		@Bean("binding_baz")
		Binding binding(@Qualifier("queue_baz") Queue queue, DirectExchange exchange) {
			System.out.println("BAZ Binding: Direct");
			return BindingBuilder.bind(queue).to(exchange).with("baz");
		}
	}
	
	@Profile("fanout")
	private static class FanoutConfig {
		@Bean("binding_baz")
		Binding binding(@Qualifier("queue_baz") Queue queue, FanoutExchange exchange) {
			System.out.println("BAZ Binding: fanout");
			return BindingBuilder.bind(queue).to(exchange);
		}
	}
}
