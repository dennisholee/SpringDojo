package io.forest.si;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerH2Repository extends JpaRepository<Customer, UUID> {

}
