package io.forest.springai.huggingface.adapter;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.forest.springai.huggingface.port.dto.MessageDto;
import io.forest.springai.huggingface.port.out.ChatAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ChatLangChainAdapter implements ChatAdapter {

	@NonNull
	ChatLanguageModel chatLanguageModel;

	@Override
	public Mono<Object> chatPrompt(MessageDto message) {
		return Mono.just(message)
				.map(MessageDto::getMessage)
				.map(UserMessage::from)
				.map(chatLanguageModel::generate)
				.map(it -> it.content()
						.text());
	}

}
