package io.forest.springai.huggingface.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.forest.springai.huggingface.application.ChatApplication;
import io.forest.springai.huggingface.application.ImageGeneratorApplication;
import io.forest.springai.huggingface.port.Chat;
import io.forest.springai.huggingface.port.ImageGenerator;
import io.forest.springai.huggingface.port.out.ChatAdapter;
import io.forest.springai.huggingface.port.out.ImageGeneratorAdapter;

public class ApplicationConf {

//	@Bean
//	Chat chat(@Autowired ChatAdapter chatAdapter) {
//		return new ChatApplication(chatAdapter);
//	}

	@Bean
	Chat chat(@Autowired ChatAdapter chatAdapter) {
		return new ChatApplication(chatAdapter);
	}

	@Bean
	ImageGenerator imageGenerator(@Autowired ImageGeneratorAdapter imageGeneratorAdapter) {
		return new ImageGeneratorApplication(imageGeneratorAdapter);
	}
}
