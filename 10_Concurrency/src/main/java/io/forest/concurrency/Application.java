package io.forest.concurrency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

import io.forest.concurrency.configuration.AdapterConf;
import io.forest.concurrency.configuration.AdapterRestApiConf;
import io.forest.concurrency.configuration.ApplicationConf;
import io.forest.concurrency.configuration.DomainConf;

@SpringBootApplication
@EnableAsync
@Import({ AdapterConf.class, AdapterRestApiConf.class, ApplicationConf.class, DomainConf.class })
public class Application {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
	}

}
