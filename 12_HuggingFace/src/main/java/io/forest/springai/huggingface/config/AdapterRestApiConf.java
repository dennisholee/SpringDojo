package io.forest.springai.huggingface.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.forest.springai.huggingface.port.Chat;
import io.forest.springai.huggingface.port.ImageGenerator;
import io.forest.springai.huggingface.port.dto.MessageDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class AdapterRestApiConf {

	@NonNull
	Chat chat;

	@NonNull
	ImageGenerator imageGenerator;

	@Bean
	RouterFunction<ServerResponse> composedNotifyRoutes() {
		return route().POST("/chat", accept(MediaType.APPLICATION_JSON), this::chat)
				.POST("/docs", accept(MediaType.MULTIPART_FORM_DATA), this::uploadDocs)
				.build();
	}

	Mono<ServerResponse> chat(ServerRequest request) {
		log.info("Request received");

		return request.bodyToMono(MessageDto.class)
				.doOnNext(n -> log.info("Received request payload [messageDto={}]", n))
				.flatMap(chat::handleChat)
				.doOnNext(it -> log.info("Chat responded [chat={}]", it))
				.flatMap(c -> ServerResponse.ok()
						.bodyValue(c));
	}

	Mono<ServerResponse> uploadDocs(ServerRequest request) {
		Flux<Object> flux = request.multipartData()
				.map(it -> it.get("image"))
				.doOnNext(i -> log.info("Image loaded"))
				.flatMapMany(Flux::fromIterable)
				.cast(FilePart.class)
				.doOnNext(i -> log.info("File name {}", i.filename()))
				.flatMap(it -> it.content())
				.map(it -> {
					byte[] raw = new byte[it.readableByteCount()];
					it.read(raw);
					return raw;
				})
				.map(createMessageDto)
				.flatMap(imageGenerator::generateImage);

		return ServerResponse.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(flux, Byte.class);
	}

	Function<byte[], MessageDto> createMessageDto = raw -> {
		MessageDto dto = new MessageDto();
		dto.setRaw(raw);
		return dto;
	};

}
