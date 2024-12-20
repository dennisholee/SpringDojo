package io.forest.hibernate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import io.forest.hibernate.conf.AdapterConf;
import io.forest.hibernate.conf.AppConf;
import io.forest.hibernate.conf.IdempotencyConf;
import io.forest.hibernate.conf.WebConfig;

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = {"io.forest.hibernate.adapter.repository", })
@EnableAspectJAutoProxy
@Import({ AdapterConf.class, AppConf.class, IdempotencyConf.class, WebConfig.class })
public class Application {

	public static void main(String args[]) {
		SpringApplication.run(Application.class, args);
	}
}
