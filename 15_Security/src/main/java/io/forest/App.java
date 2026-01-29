package io.forest;

import io.forest.security.conf.AppConf;
import io.forest.security.conf.SwaggerConf;
import io.forest.security.conf.WebSecurityConf;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
		SwaggerConf.class,
		AppConf.class,
		WebSecurityConf.class
})
public class App {
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
