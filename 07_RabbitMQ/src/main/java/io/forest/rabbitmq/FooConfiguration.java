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
public class FooConfiguration {

	@Value("${rabbitmq.queues.foo}")
	String queueName;

	@Bean("queue_foo")
	Queue queue() {
		return new Queue(queueName, false);
	}

	@Bean("container_foo")
	SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
			@Qualifier("listener_adapter_foo") MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(queueName);
		container.setMessageListener(listenerAdapter);
		return container;
	}

	@Bean("listener_adapter_foo")
	MessageListenerAdapter listenerAdapter(FooReceiver receiver) {
		return new MessageListenerAdapter(receiver, "receiveMessage");
	}

	@Profile("topic")
	private static class TopicConfig {
		
		@Bean("binding_foo")
		Binding binding(@Qualifier("queue_foo") Queue queue, TopicExchange exchange) {
			System.out.println("Foo Binding: Topic");
			return BindingBuilder.bind(queue).to(exchange).with("foo.#");
		}
	}

	@Profile("headers")
	private static class HeadersConfig {
		
		@Bean("binding_foo")
		Binding binding(@Qualifier("queue_foo") Queue queue, HeadersExchange exchange) {
			System.out.println("Foo Binding: Headers");
			return BindingBuilder.bind(queue).to(exchange).where("key").matches("foo");
		}
	}
	
	@Profile("direct")
	private static class DirectConfig {
		@Bean("binding_foo")
		Binding binding(@Qualifier("queue_foo") Queue queue, DirectExchange exchange) {
			System.out.println("FOO Binding: Direct");
			return BindingBuilder.bind(queue).to(exchange).with("foo");
		}
	}
	
	@Profile("fanout")
	private static class FanoutConfig {
		@Bean("binding_foo")
		Binding binding(@Qualifier("queue_foo") Queue queue, FanoutExchange exchange) {
			System.out.println("FOO Binding: Fanout");
			return BindingBuilder.bind(queue).to(exchange);
		}
	}
}
