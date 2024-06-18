package io.forest.txnoutbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import io.forest.txnoutbox.conf.AdapterDBConf;
import io.forest.txnoutbox.conf.AdapterRestApiConf;
import io.forest.txnoutbox.conf.ApplicationConf;

@SpringBootApplication
@Import(value = { AdapterDBConf.class, AdapterRestApiConf.class, ApplicationConf.class })
public class Application {

	public static void main(String args[]) {
		SpringApplication.run(Application.class, args);
	}
}
