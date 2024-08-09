package io.forest.si;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConsentsH2Repository extends JpaRepository<Consents, UUID> {

	@Query
	Consents findByCustomerIdAndConsentType(UUID customerId, ConsentType consentType);
}
