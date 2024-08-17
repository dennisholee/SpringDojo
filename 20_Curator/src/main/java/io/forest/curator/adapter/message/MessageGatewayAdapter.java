package io.forest.curator.adapter.message;

import io.forest.curator.port.MessageGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class MessageGatewayAdapter implements MessageGateway {

	@Override
	public Object enqueue(Object message) {
		log.info("Enqueue message [payload={}]", message);
		
		return message;
	}
}
