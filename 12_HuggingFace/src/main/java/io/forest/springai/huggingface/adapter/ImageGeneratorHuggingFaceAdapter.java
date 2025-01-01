package io.forest.springai.huggingface.adapter;

import java.util.Base64;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.ImageContent.DetailLevel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.forest.springai.huggingface.port.dto.MessageDto;
import io.forest.springai.huggingface.port.out.ImageGeneratorAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ImageGeneratorHuggingFaceAdapter implements ImageGeneratorAdapter {

	@NonNull
	ChatLanguageModel chatLanguageModel;

	public Mono<Object> generateImage(MessageDto messageDto) {

//		byte[] raw = messageDto.getRaw();
//
//		String base64Raw = Base64.getEncoder()
//				.encodeToString(raw);
//
//		UserMessage.from(ImageContent.from(base64Raw, "image/jpg", DetailLevel.AUTO));

	
		return Mono.just(messageDto)
				.map(MessageDto::getRaw)
				.map(Base64.getEncoder()::encodeToString)
				.map(it -> ImageContent.from(it, "image/jpg", DetailLevel.AUTO))
				.map(UserMessage::from)
				.map(chatLanguageModel::generate)
				.map(it -> it.content()
						.text());
	}
}
