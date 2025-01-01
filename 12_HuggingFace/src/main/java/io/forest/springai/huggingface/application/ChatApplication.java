package io.forest.springai.huggingface.application;

import java.util.Map;

import io.forest.springai.huggingface.port.Chat;
import io.forest.springai.huggingface.port.dto.MessageDto;
import io.forest.springai.huggingface.port.out.ChatAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class ChatApplication implements Chat {

	@NonNull
	ChatAdapter adapter;

//	public Mono<Map<String, String>> handleRequest(MessageDto message) {
//		return Mono.just(message)
//				.doOnNext(it -> log.info("Processing in chat application [messageDto={}]", it))
//				.flatMap(adapter::chat);
//	}
	
	public Mono<Object> handleChat(MessageDto message) {
		return Mono.just(message)
				.doOnNext(it -> log.info("Processing in chat application [messageDto={}]", it))
				.flatMap(adapter::chatPrompt);
	}
	
	
}
