package io.forest.springai.huggingface.config;

import static dev.langchain4j.model.huggingface.HuggingFaceModelName.TII_UAE_FALCON_7B_INSTRUCT;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import io.forest.springai.huggingface.adapter.ChatLangChainAdapter;
import io.forest.springai.huggingface.adapter.ImageGeneratorHuggingFaceAdapter;
import io.forest.springai.huggingface.port.out.ChatAdapter;
import io.forest.springai.huggingface.port.out.ImageGeneratorAdapter;

public class AdapterConf {

	@Value("${application.ai.huggingface.chat.api-key}")
	String huggingFaceAccessToken;

	@Bean(name = "chatModel")
	ChatLanguageModel chatLanguageModel() {
		return HuggingFaceChatModel.builder()
				.accessToken(huggingFaceAccessToken)
				.modelId(TII_UAE_FALCON_7B_INSTRUCT)
				.returnFullText(true)
				.timeout(Duration.ofSeconds(15))
				.temperature(0.7)
				.maxNewTokens(20)
				.waitForModel(true)
				.build();
	}

	@Bean(name = "image2TextModel")
	ChatLanguageModel image2TextModel() {
		return HuggingFaceChatModel.builder()
				.accessToken(huggingFaceAccessToken)
				.modelId("Salesforce/blip-image-captioning-base")
				.timeout(Duration.ofSeconds(15))
				.temperature(0.7)
				.maxNewTokens(20)
				.waitForModel(true)
				.build();
	}

	@Bean
	ChatAdapter chatLangChainAdapter(@Autowired @Qualifier("chatModel") ChatLanguageModel chatLanguageModel) {
		return new ChatLangChainAdapter(chatLanguageModel);
	}

	@Bean
	ImageGeneratorAdapter imageGeneratorAdapter(@Autowired @Qualifier("image2TextModel") ChatLanguageModel chatLanguageModel) {
		return new ImageGeneratorHuggingFaceAdapter(chatLanguageModel);
	}
}
