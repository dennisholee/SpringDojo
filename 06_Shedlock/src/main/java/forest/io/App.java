package forest.io;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mongodb.client.MongoCollection;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * Hello world!
 *
 */

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class App {

	@Autowired
	private MongoTemplate template;

	@Bean
	public LockProvider lockProvider() {
		MongoCollection<Document> mongo = template.getCollection("shed_lock");
		return new MongoLockProvider(mongo);
	}

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
