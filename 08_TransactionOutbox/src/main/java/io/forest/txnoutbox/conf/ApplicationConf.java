package io.forest.txnoutbox.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.forest.txnoutbox.application.CreateClaimApplication;
import io.forest.txnoutbox.port.out.ClaimRepository;
import io.forest.txnoutbox.port.out.OutboxRepository;

public class ApplicationConf {

	@Bean
	CreateClaimApplication createClaimApplication(	@Autowired ClaimRepository claimRepository,
													@Autowired OutboxRepository outboxRepository) {
		return new CreateClaimApplication(claimRepository, outboxRepository);
	}
}
