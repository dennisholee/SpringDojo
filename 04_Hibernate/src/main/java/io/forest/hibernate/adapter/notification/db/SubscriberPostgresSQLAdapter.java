package io.forest.hibernate.adapter.notification.db;

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SubscriberPostgresSQLAdapter<T> {

	@NonNull
	String channel;

	@NonNull
	JdbcTemplate jdbcTemplate;

	public Function<Consumer<PGNotification>, Runnable> createNotificationHandler = consumer -> () -> this.jdbcTemplate
			.execute((Connection c) -> {

				c.createStatement()
						.execute(String.format("LISTEN %s", this.channel));
				PGConnection connection = c.unwrap(PGConnection.class);

				while (!Thread.currentThread()
						.isInterrupted()) {
					PGNotification[] nts = connection.getNotifications(10000);
					if (nts == null || nts.length == 0) {
						continue;
					}
					for (PGNotification nt : nts) {
						consumer.accept(nt);
					}
				}

				return 0;
			});

}
