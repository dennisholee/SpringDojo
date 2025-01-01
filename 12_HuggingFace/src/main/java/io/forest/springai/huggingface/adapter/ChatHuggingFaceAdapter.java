package io.forest.springai.huggingface.adapter;

import java.util.Map;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.huggingface.HuggingfaceChatModel;

import io.forest.springai.huggingface.port.dto.MessageDto;
import io.forest.springai.huggingface.port.out.ChatAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log4j2
public class ChatHuggingFaceAdapter implements ChatAdapter {

	@NonNull
	HuggingfaceChatModel model;

	public Mono<Map<String, String>> chat(MessageDto message) {
		return Mono.just(message)
				.map(MessageDto::getMessage)
				.doOnNext(it -> log.info("Calling model [message={}]", it))
				.map(model::call)
				.doOnNext(it -> log.info("Model responded [response={}]", it))
				.map(it -> Map.of("generation", it));
	}
	
	public Mono<Object> chatPrompt(MessageDto message) {
		return Mono.just(message)
				.map(MessageDto::getMessage)
				.doOnNext(it -> log.info("Calling model [message={}]", it))
				.map(it -> new Prompt(it))
				.map(model::call)
				.doOnNext(it -> log.info("Model responded [response={}]", it))
				.map(it -> Map.of("generation", it));
		
	}
}
