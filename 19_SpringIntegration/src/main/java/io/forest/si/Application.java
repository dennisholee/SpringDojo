package io.forest.si;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.http.dsl.Http;

@SpringBootApplication
@EnableIntegration
public class Application {

	@Bean(name = "AddressEnricher.inChannel")
	DirectChannelSpec a() {
		return MessageChannels.direct();
	}

	@Bean(name = "AddressEnricher.outChannel")
	DirectChannelSpec b() {
		return MessageChannels.direct();
	}

	@Bean
	DirectChannelSpec c() {
		return MessageChannels.direct();
	}

	@Bean(name = "OptInEnricher.inChannel")
	DirectChannelSpec d() {
		return MessageChannels.direct();
	}

	@Bean(name = "OptInEnricher.outChannel")
	DirectChannelSpec e() {
		return MessageChannels.direct()
				.wireTap("logFlow.input");
	}

	@Bean(name = "OptInFlow.channel")
	DirectChannelSpec outInFlowChannel() {
		return MessageChannels.direct();
	}

	@Bean(name = "OptOutFlow.channel")
	DirectChannelSpec outOutFlowChannel() {
		return MessageChannels.direct();
	}

	@Bean(name = "aggregateChannel")
	DirectChannelSpec aggregateChannel() {
		return MessageChannels.direct();
	}

	@Bean(name = "errorChannel")
	DirectChannelSpec errorChannel() {
		return MessageChannels.direct();
	}

	@Bean(name = "outChannel")
	DirectChannelSpec outChannel() {
		return MessageChannels.direct();
	}

	@Bean(name = "CustomerListServiceActivator.inChannel")
	DirectChannelSpec customerListServiceActivatorInChannel() {
		return MessageChannels.direct();
	}

	@Bean(name = "CustomerListServiceActivator.outChannel")
	DirectChannelSpec customerListServiceActivatorOutChannel() {
		return MessageChannels.direct();
	}

	@Bean
	DirectChannelSpec y() {
		return MessageChannels.direct();
	}

	@Bean
	DirectChannelSpec z() {
		return MessageChannels.direct();
	}

	@Autowired(required = true)
	OptInFilter optInFilter;

	@Bean
	IntegrationFlow foo() {
		return IntegrationFlow.from(Http.inboundGateway("/foo")
				.requestMapping(r -> r.methods(HttpMethod.GET))
				.requestPayloadType(String.class)
				.replyChannel("z"))
				.channel("a")
				.enrich(e -> e.requestChannel("OptInEnricher.inChannel")
						.replyChannel("OptInEnricher.outChannel")
						.propertyExpression("['OptIn']", "payload.optIn"))
				.route("payload['OptIn']",
						r -> r.subFlowMapping(Boolean.TRUE, s -> s.gateway(optInFlow()))
								.subFlowMapping(Boolean.FALSE, s -> s.gateway(optOutFlow())))
				.get();
	}

	@Bean(name = "myFlow.outChannel")
	DirectChannelSpec acmeChannel() {
		return MessageChannels.direct();
	}

	@Bean
	IntegrationFlow bar(@Autowired(required = true) CustomerServiceActivator customerListServiceActivator) {
		ExecutorService exec = Executors.newFixedThreadPool(10);

		return IntegrationFlow.from(Http.inboundGateway("/users")
				.requestMapping(r -> r.methods(HttpMethod.GET))
				.errorChannel("errorChannel")
				.replyChannel("outChannel"))
				.channel(new DirectChannel())
				.handle(customerListServiceActivator)
				.split("payload")
				.channel(c -> c.executor(exec))
				.transform(Transformers.toMap())
				.enrich(e -> e.requestChannel("OptInEnricher.inChannel")
						.replyChannel("OptInEnricher.outChannel")
						.propertyExpression("['OptIn']", "payload.optIn"))
				.route("payload['OptIn']",
						r -> r.subFlowMapping(true, s -> s.gateway(optInFlow()))
								.subFlowMapping(false, s -> s.gateway(optOutFlow())))
				.log(Level.INFO, Application.class.getCanonicalName(), m -> String.format("Payload [%s]", m))
				.channel("aggregateChannel")
				.aggregate()
				.channel("outChannel")
				.get();
	}

	@Bean
	IntegrationFlow optInFlow() {
		return f -> f.enrich(e -> e.requestChannel("AddressEnricher.inChannel")
				.propertyExpression("['Address']", "payload"));
	}

	@Bean
	IntegrationFlow optOutFlow() {
		return f -> f.bridge();
	}

	@Bean
	IntegrationFlow logFlow() {
		return f -> f.log();
	}

	@Bean
	IntegrationFlow errorHandler() {
		return f -> f.channel("errorChannel")
				.log()
				.channel(new NullChannel());
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
