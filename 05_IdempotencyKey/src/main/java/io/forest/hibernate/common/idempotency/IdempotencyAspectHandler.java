package io.forest.hibernate.common.idempotency;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.forest.hibernate.common.idempotency.key.IdempotencyKeyValidator;
import io.forest.hibernate.common.idempotency.repository.RequestLog;
import io.forest.hibernate.common.idempotency.repository.RequestLogRepository;
import io.forest.hibernate.common.idempotency.repository.State;
import io.forest.hibernate.common.idempotency.resolver.IdempotencyKeyResolver;
import io.forest.hibernate.common.idempotency.resolver.ServiceTypeResolver;
import io.forest.hibernate.common.idempotency.response.StateResponseFactory;
import io.forest.hibernate.common.idempotency.throwable.IdempotencyKeyInvalidException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Aspect
@RequiredArgsConstructor
@Log4j2
public class IdempotencyAspectHandler {

	@NonNull
	IdempotencyKeyResolver idempotencyKeyResolver;

	@NonNull
	IdempotencyKeyValidator idempotentKeyValidator;

	@NonNull
	ServiceTypeResolver serviceTypeResolver;

	@NonNull
	RequestLogRepository repository;

	@NonNull
	StateResponseFactory requestStateResponseFactory;

	public Object handlePointcut(ProceedingJoinPoint joinPoint) throws Throwable {
		String resolvedIdempotencykey = idempotencyKeyResolver.resolve(joinPoint)
				.filter(idempotentKeyValidator::isValid)
				.map(String::valueOf)
				.orElseThrow(() -> new IdempotencyKeyInvalidException());

		log.info("Resolved idempotent key [key={}]", resolvedIdempotencykey);

		return Mono.just(resolvedIdempotencykey)
				.map(UUID::fromString)
				.flatMap(repository::findById)
				.switchIfEmpty(Mono.defer(() -> createRequestLog(joinPoint, resolvedIdempotencykey)))
				.map(it -> triageRequest(it));
		// .map(this::handleRequest);

//		if (requestLog.isPresent()) {
//			requestLog.ifPresent(it -> log.info("Request found in log [state={}]", it.getRequestState()));
//
//			return requestLog.map(toResponseEntity);
//		}

//		String serviceType = serviceTypeResolver.resolve(joinPoint);
//
//		ObjectMapper objectMapper = new ObjectMapper();
//		String argsJson = objectMapper.writeValueAsString(joinPoint.getArgs());
//
//		RequestLog currentRequestLog = new RequestLog();
//		currentRequestLog.setIdempotencyKey(resolvedIdempotencykey);
//		currentRequestLog.setServiceType(serviceType);
//		currentRequestLog.setRequestState(State.WIP);
//		currentRequestLog.setRequestPayload(argsJson);
//
//		this.repository.save(currentRequestLog);

//		Object payload = joinPoint.proceed();
//
//		ResponseEntity<Object> responseEntity = (ResponseEntity<Object>) payload;
//		// String responseJson =
//
//		return null;
	}

	Mono<RequestLog> createRequestLog(	ProceedingJoinPoint joinPoint,
										String resolvedIdempotencykey) {
		String serviceType = serviceTypeResolver.resolve(joinPoint);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String argsJson = objectMapper.writeValueAsString(joinPoint.getArgs());

			RequestLog currentRequestLog = new RequestLog().setIdempotencyKey(resolvedIdempotencykey)
					.setServiceType(serviceType)
					.setRequestState(State.NEW)
					.setRequestPayload(argsJson);
			return Mono.just(currentRequestLog);
		} catch (JsonProcessingException e) {
			return Mono.error(e);
		}
	}

	Mono<ResponseEntity> triageRequest(RequestLog requestLog) {
		return Mono.just(requestLog)
				.map(it -> switch (it.getRequestState()) {
				case WIP, SUCCEED, FAILED -> toResponseEntity.apply(it);
				case NEW -> null;
				default -> null;
				});

	}

	Mono<Object> processRequest(ProceedingJoinPoint joinPoint,
								RequestLog requestLog) {
		ObjectMapper objectMapper = new ObjectMapper();
		return Mono.just(requestLog)
				.flatMap(repository::save)
				.map(it -> proceed(joinPoint));
//				.map(objectMapper::writeValueAsString);

	}

	Mono<ResponseEntity> proceed(ProceedingJoinPoint joinPoint) {
		try {
			return (Mono<ResponseEntity>) joinPoint.proceed();
		} catch (Throwable e) {
			return Mono.error(e);
		}
	}

	Function<RequestLog, ResponseEntity> toResponseEntity = r -> requestStateResponseFactory
			.createResponse(r.getRequestState())
			.toResponseEntity(r);

}
