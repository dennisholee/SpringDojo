package io.forest.curator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.forest.curator.conf.AdapterConf;
import io.forest.curator.conf.ApplicationConf;
import io.forest.curator.conf.CuratorConf;

@SpringBootApplication
@EnableJpaRepositories
@Import(value = { AdapterConf.class, ApplicationConf.class, CuratorConf.class })
public class Application {

	public static void main(String[] args) throws Exception {

		SpringApplication.run(Application.class, args);
	}

}
