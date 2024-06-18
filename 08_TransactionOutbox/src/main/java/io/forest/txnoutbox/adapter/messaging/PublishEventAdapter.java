package io.forest.txnoutbox.adapter.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.encoder.JsonEncoder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PublishEventAdapter {

	@NonNull
	KafkaTemplate kafkaTemplate;

	public void publishEvent(Object event) {
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			byte[] raw = objectMapper.writeValueAsBytes(event);
			kafkaTemplate.send("my-topic", raw);

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}
