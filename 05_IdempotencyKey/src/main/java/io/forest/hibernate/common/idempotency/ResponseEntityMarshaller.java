package io.forest.hibernate.common.idempotency;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseEntityMarshaller {

	public String marshall(ResponseEntity responseEntity) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		HttpStatusCode statusCode = responseEntity.getStatusCode();

		Map<String, String> headers = new HashMap<>();
		for (Entry<String, List<String>> entry : responseEntity.getHeaders()
				.entrySet()) {
			headers.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
		}

		Object body = responseEntity.getBody();
		String jsonBody = objectMapper.writeValueAsString(body);

		Map<String, Object> response = Map.of("statusCode", statusCode.value(), "headers", headers, "body", jsonBody);
		return objectMapper.writeValueAsString(response);
	}

}
