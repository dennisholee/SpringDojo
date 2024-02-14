package io.forest.hibernate.conf;

import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.util.DriverDataSource;

import io.forest.hibernate.adapter.notification.db.NotifierPostgresSQLAdapter;
import io.forest.hibernate.adapter.notification.db.SubscriberPostgresSQLAdapter;
import io.forest.hibernate.adapter.repository.sql.NotificationJpaRepository;
import io.forest.hibernate.adapter.repository.sql.NotificationSqlRepository;
import io.forest.hibernate.port.out.NotificationRepository;
import io.forest.hibernate.port.out.NotifierAdapter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AdapterConf {

	@Value("${application.channel}")
	String channel;

	@Bean
	NotifierAdapter notifierAdapter(DataSourceProperties dataSourceProperties) {
		JdbcTemplate jdbcTemplate = createDriverDataSource.andThen(createJdbcTemplate)
				.apply(dataSourceProperties)
				.get();

		return new NotifierPostgresSQLAdapter(channel, jdbcTemplate);
	}

	@Bean
	SubscriberPostgresSQLAdapter<String> subscriberPostgresSQLAdapter(DataSourceProperties dataSourceProperties) {
		JdbcTemplate jdbcTemplate = createDriverDataSource.andThen(createJdbcTemplate)
				.apply(dataSourceProperties)
				.get();

		SubscriberPostgresSQLAdapter<String> subscriberPostgresSQLAdapter = new SubscriberPostgresSQLAdapter<String>(
				channel,
				jdbcTemplate);

		return subscriberPostgresSQLAdapter;
	}

	@Bean
	TaskExecutor taskExecutor() {
		return new SimpleAsyncTaskExecutor();
	}

	@Bean
	CommandLineRunner schedulingRunner(	@Autowired TaskExecutor executor,
										@Autowired SubscriberPostgresSQLAdapter<String> subscriberPostgresSQLAdapter) {
		return args -> {
			executor.execute(subscriberPostgresSQLAdapter.createNotificationHandler.apply(msg -> {
				log.info("Listener > name={} parameter={} pid={}", msg.getName(), msg.getParameter(), msg.getPID());
			}));
		};
	}

	@Bean
	NotificationRepository notificationRepository(@Autowired NotificationJpaRepository notificationJpaRepository) {
		return new NotificationSqlRepository(notificationJpaRepository);
	}

	static Function<DataSourceProperties, DriverDataSource> createDriverDataSource = dataSourceProperties -> new DriverDataSource(
			dataSourceProperties.determineUrl(),
			dataSourceProperties.determineDriverClassName(),
			new Properties(),
			dataSourceProperties.determineUsername(),
			dataSourceProperties.determinePassword());

	static Function<DriverDataSource, Supplier<JdbcTemplate>> createJdbcTemplate = driverDataSource -> () -> new JdbcTemplate(
			driverDataSource);
}
