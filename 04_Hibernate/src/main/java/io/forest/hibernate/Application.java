package io.forest.hibernate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import io.forest.hibernate.conf.AdapterConf;
import io.forest.hibernate.conf.ApplicationConf;
import io.forest.hibernate.conf.MessageAPIAdapter;

@SpringBootApplication
@EnableJpaRepositories(enableDefaultTransactions = false)
@EnableAsync
@Import({ ApplicationConf.class, AdapterConf.class, MessageAPIAdapter.class })
public class Application {

	public static void main(String args[]) {
		SpringApplication.run(Application.class, args);
	}
}
