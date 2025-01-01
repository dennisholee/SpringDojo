package io.forest.springai.huggingface.port;

import io.forest.springai.huggingface.port.dto.MessageDto;
import reactor.core.publisher.Mono;

public interface Chat {
	 Mono<Object> handleChat(MessageDto message);
}
