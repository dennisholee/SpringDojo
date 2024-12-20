package io.forest.hibernate.common.idempotency;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.json.JSONObject;
import org.springframework.boot.logging.LogLevel;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.forest.hibernate.common.idempotency.repository.RequestLog;
import io.forest.hibernate.common.idempotency.repository.RequestLogRepository;
import io.forest.hibernate.common.idempotency.repository.State;
import io.forest.hibernate.common.idempotency.response.StateResponseFactory;
import io.forest.hibernate.common.idempotency.throwable.IdempotencyKeyNotDefinedException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class IdempotencyFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

	@NonNull
	StateResponseFactory requestStateResponseFactory;

	@NonNull
	RequestLogRepository requestLogRepository;

	@Override
	public Mono<ServerResponse> filter(	ServerRequest request,
										HandlerFunction<ServerResponse> next) {

		Mono<String> idempotencyKey = Mono.just(request)
				.map(ServerRequest::headers)
				.map(it -> it.header("idempotency-key"))
				.filter(Predicate.not(List::isEmpty))
				.map(it -> it.get(0))
				.doOnNext(it -> log.info("Idempotency key found in HTTP header [key={}]", it))
				.switchIfEmpty(Mono.error(new IdempotencyKeyNotDefinedException()))
				.onErrorStop();

//		String idempotencyKey = request.headers()
//				.header("idempotency-key")
//				.get(0);

		Mono<RequestLog> requestLog = idempotencyKey // Mono.justOrEmpty(idempotencyKey)
				.map(UUID::fromString)
				.flatMap(requestLogRepository::findById)
				.switchIfEmpty(Mono.defer(() -> createRequestLog(request, idempotencyKey)));

		requestLog.filter(it -> it.getRequestState() != State.NEW)
				.flatMap(requestLogRepository::save)
				.flatMap(it -> {
					return next.handle(request)
							.cast(EntityResponse.class)
							.map(rsp -> rsp.entity())
							.map(JSONObject::valueToString);

				})
				.subscribe(it -> requestLog.doOnNext(r -> {
					r.setResponsePayload(it);
					r.setRequestState(State.SUCCEED);
				}));

		return requestLog.map(it -> requestStateResponseFactory.createResponse(it.getRequestState())
				.toResponseEntity(it))
				.cast(ServerResponse.class);
	}

	Mono<RequestLog> createRequestLog(	ServerRequest request,
										Mono<String> idempotencyKey) {
		Mono<RequestLog> log = request.bodyToMono(JSONObject.class)
				.map(it -> new RequestLog()// .setIdempotencyKey(idempotencyKey)
						.setServiceType(String.format("%s:%s", request.method(), request.path()))
						.setRequestState(State.NEW)
						.setRequestPayload(it.toString()));
		return Mono.zip(log,
				idempotencyKey,
				(	l,
					k) -> l.setIdempotencyKey(k));
	}

//	Function<RequestLog, ResponseEntity> toResponseEntity = r -> requestStateResponseFactory
//			.createResponse(r.getRequestState())
//			.toResponseEntity(r);
}
