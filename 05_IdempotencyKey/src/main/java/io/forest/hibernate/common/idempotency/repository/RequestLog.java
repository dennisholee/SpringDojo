package io.forest.hibernate.common.idempotency.repository;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Table("request_log")
@Accessors(chain = true)
public class RequestLog implements Serializable {

	@Id
	@Column("IDEMPOTENCY_KEY")
	String idempotencyKey;

	@Column("request_state")
	State requestState;

	@Column("service_type")
	String serviceType;

	@Column("request_payload")
	String requestPayload;

	@Column("response_payload")
	String responsePayload;

	@Version
	@Column("version")
	long version;

}
