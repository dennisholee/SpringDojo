package io.forest.txnoutbox.conf;

import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.util.DriverDataSource;

import io.forest.txnoutbox.adapter.repository.sql.ClaimJpaRepository;
import io.forest.txnoutbox.adapter.repository.sql.ClaimSqlRepository;
import io.forest.txnoutbox.adapter.repository.sql.OutboxJpaRepository;
import io.forest.txnoutbox.adapter.repository.sql.OutboxSqlRepository;
import io.forest.txnoutbox.adapter.repository.sql.model.ClaimEntityListener;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AdapterDBConf {

	@Bean
	ClaimSqlRepository claimRepository(@Autowired ClaimJpaRepository claimJpaRepository) {
		return new ClaimSqlRepository(claimJpaRepository);
	}

	@Bean
	OutboxSqlRepository outboxRepository(@Autowired OutboxJpaRepository outboxJpaRepository) {
		return new OutboxSqlRepository(outboxJpaRepository);
	}

	@Bean
	ClaimEntityListener claimEntityListener() {
		return new ClaimEntityListener();
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
