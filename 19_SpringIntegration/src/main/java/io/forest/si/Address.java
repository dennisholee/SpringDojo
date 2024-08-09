package io.forest.si;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "ADDRESSES")
@NoArgsConstructor
@Data
@ToString
public class Address {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ADDRESS_ID")
	UUID addressId;
	
	@Column(name = "CUSTOMER_ID")
	UUID customerId;

	@Column(name = "STREET")
	String street;

}
