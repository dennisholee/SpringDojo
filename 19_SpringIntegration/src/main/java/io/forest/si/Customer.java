package io.forest.si;

import static jakarta.persistence.GenerationType.UUID;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.ToString;

@Entity(name = "CUSTOMERS")
@Data
@ToString
public class Customer {

	@Id
	@Column(name = "CUSTOMER_ID")
	@GeneratedValue(strategy = UUID)
	UUID id;

	@Column(name = "FIRSTNAME",
			length = 50)
	String firstName;

	@Column(name = "LASTNAME",
			length = 50)
	String lastName;
}
