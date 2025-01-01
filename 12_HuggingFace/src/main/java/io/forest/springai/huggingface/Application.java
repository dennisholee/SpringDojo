package io.forest.springai.huggingface;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import io.forest.springai.huggingface.config.AdapterConf;
import io.forest.springai.huggingface.config.AdapterRestApiConf;
import io.forest.springai.huggingface.config.ApplicationConf;

@SpringBootApplication
@Import({ AdapterConf.class, AdapterRestApiConf.class, ApplicationConf.class })
public class Application {

	public static void main(String args[]) {
		SpringApplication.run(Application.class, args);
	}
}
