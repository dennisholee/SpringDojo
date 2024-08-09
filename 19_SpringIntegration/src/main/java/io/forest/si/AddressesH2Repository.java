package io.forest.si;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressesH2Repository extends JpaRepository<Address, UUID> {

	Optional<Address> findByCustomerId(UUID userId);

}
