package io.forest.si;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class CustomerServiceActivator {

	@Autowired(required = true)
	CustomerH2Repository repository;

	@ServiceActivator
	List<Customer> getCustomers(Message<?> message) {
		List<Customer> customers = this.repository.findAll();

		log.info("Customer list record retrieved [size={}]", customers.size());

		return customers;
	}
}
