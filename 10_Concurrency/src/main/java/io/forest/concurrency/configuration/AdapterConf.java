package io.forest.concurrency.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import io.forest.concurrency.adapter.repository.ConsentMapper;
import io.forest.concurrency.adapter.repository.ConsentMapperImpl;
import io.forest.concurrency.adapter.repository.ConsentRepositoryAdapter;
import io.forest.concurrency.adapter.repository.h2.ConsentH2Repository;

public class AdapterConf {

	@Bean("Application.ConsentMapper")
	ConsentMapper mapper() {
		return new ConsentMapperImpl();
	}

	@Bean
	ConsentRepositoryAdapter consentRepositoryAdapter(	@Autowired ConsentH2Repository repository,
														@Autowired @Qualifier("Application.ConsentMapper") ConsentMapper mapper) {
		return new ConsentRepositoryAdapter(repository, mapper);
	}
}
