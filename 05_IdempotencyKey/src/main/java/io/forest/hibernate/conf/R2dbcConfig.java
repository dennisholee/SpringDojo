package io.forest.hibernate.conf;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import io.forest.hibernate.common.idempotency.converter.RequestStateReadConverter;
import io.forest.hibernate.common.idempotency.converter.RequestStateWriteConverter;
import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;

public class R2dbcConfig extends AbstractR2dbcConfiguration {

	@Value("${application.database.url}")
	String databaseUrl;

	@Override
	public ConnectionFactory connectionFactory() {
		new H2ConnectionFactory(
				H2ConnectionConfiguration.builder()
						.url(databaseUrl)
						.build());
		return null;
	}

	@Override
	protected List<Object> getCustomConverters() {
		return List.of(new RequestStateReadConverter(), new RequestStateWriteConverter());
	}

}
