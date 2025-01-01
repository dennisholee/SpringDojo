package io.forest.springai.huggingface.application;

import io.forest.springai.huggingface.port.ImageGenerator;
import io.forest.springai.huggingface.port.dto.MessageDto;
import io.forest.springai.huggingface.port.out.ImageGeneratorAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ImageGeneratorApplication implements ImageGenerator {

	@NonNull
	ImageGeneratorAdapter imageGenerator;

	public Mono<Object> generateImage(MessageDto messageDto) {
		return Mono.just(messageDto)
				.map(imageGenerator::generateImage);

	}
}
