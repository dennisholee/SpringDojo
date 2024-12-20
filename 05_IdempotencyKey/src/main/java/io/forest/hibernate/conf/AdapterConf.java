package io.forest.hibernate.conf;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.H2Dialect;

import io.forest.hibernate.adapter.api.HomeRestController;
import io.forest.hibernate.adapter.repository.PostRepositoryAdapter;
import io.forest.hibernate.adapter.repository.h2.PostH2Repository;
import io.forest.hibernate.common.idempotency.converter.RequestStateReadConverter;
import io.forest.hibernate.common.idempotency.converter.RequestStateWriteConverter;
import io.forest.hibernate.port.PostRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AdapterConf {

	@Bean
	HomeRestController homeRestController() {
		return new HomeRestController();
	}

	@Bean
	PostRepository postRepository(PostH2Repository repository) {
		return new PostRepositoryAdapter(repository);
	}
	
	@Bean
	public R2dbcCustomConversions customConversions() {
		return R2dbcCustomConversions.of(H2Dialect.INSTANCE,
				List.of(new RequestStateReadConverter(), new RequestStateWriteConverter()));
	}
}
