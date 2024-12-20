package io.forest.hibernate.conf;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import io.forest.hibernate.common.idempotency.IdempotencyAspectHandler;
import io.forest.hibernate.common.idempotency.key.IdempotencyKeyValidator;
import io.forest.hibernate.common.idempotency.key.UUIDIdempotencyKeyValidator;
import io.forest.hibernate.common.idempotency.repository.RequestLogRepository;
import io.forest.hibernate.common.idempotency.repository.State;
import io.forest.hibernate.common.idempotency.resolver.IdempotencyKeyResolver;
import io.forest.hibernate.common.idempotency.resolver.ServiceTypeResolver;
import io.forest.hibernate.common.idempotency.response.FailedRequestStateResponse;
import io.forest.hibernate.common.idempotency.response.StateResponse;
import io.forest.hibernate.common.idempotency.response.StateResponseFactory;
import io.forest.hibernate.common.idempotency.response.SuccessRequestStateResponse;
import io.forest.hibernate.common.idempotency.response.WIPRequestStateResponse;

@EnableR2dbcRepositories(basePackages = "io.forest.hibernate.common.idempotency.repository")
public class IdempotencyConf {

	@Bean
	IdempotencyKeyResolver idempotencyKeyResolver() {
		return new IdempotencyKeyResolver();
	}

	@Bean
	IdempotencyKeyValidator uuidIdempotencyKeyValidator() {
		return new UUIDIdempotencyKeyValidator();
	}

	@Bean
	ServiceTypeResolver serviceTypeResolver() {
		return new ServiceTypeResolver();
	}

	@Bean("requestStateResponseFactory")
	FactoryBean<Object> serviceLocatorFactoryBean() {
		ServiceLocatorFactoryBean factoryBean = new ServiceLocatorFactoryBean();
		factoryBean.setServiceLocatorInterface(StateResponseFactory.class);
		return factoryBean;
	}

	@Bean(State.TypeConstant.WIP)
	WIPRequestStateResponse wipRequestStateResponse() {
		return new WIPRequestStateResponse();
	}

	@Bean(State.TypeConstant.SUCCEED)
	SuccessRequestStateResponse successRequestStateResponse() {
		return new SuccessRequestStateResponse();
	}

	@Bean(State.TypeConstant.FAILED)
	FailedRequestStateResponse failedRequestStateResponse() {
		return new FailedRequestStateResponse();
	}

	@Bean
	IdempotencyAspectHandler idempotencyAspectHandler(	@Autowired(required = true) IdempotencyKeyResolver idempotencyKeyResolver,
														@Autowired(required = true) IdempotencyKeyValidator idempotencyKeyValidator,
														@Autowired(required = true) ServiceTypeResolver serviceTypeResolver,
														@Autowired(required = true) RequestLogRepository requestLogRepository,
														@Autowired(required = true) StateResponseFactory stateResponseFactory) {
		return new IdempotencyAspectHandler(
				idempotencyKeyResolver,
				idempotencyKeyValidator,
				serviceTypeResolver,
				requestLogRepository,
				stateResponseFactory);
	}
}
