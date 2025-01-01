package io.forest.springai.huggingface.port.out;

import io.forest.springai.huggingface.port.dto.MessageDto;
import reactor.core.publisher.Mono;

public interface ImageGeneratorAdapter {

	Mono<Object> generateImage(MessageDto messageDto);
}
