package io.forest.hibernate.conf;

import org.springframework.context.annotation.Bean;

import io.forest.hibernate.application.SavePostApp;
import io.forest.hibernate.port.PostRepository;
import io.forest.hibernate.port.SavePost;
import lombok.NonNull;

public class AppConf {

	@Bean
	SavePost savePost(@NonNull PostRepository postRepository) {
		return new SavePostApp(postRepository);
	}

}
