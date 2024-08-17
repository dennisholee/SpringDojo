package io.forest.curator.port;

public interface MessageGateway {

	Object enqueue(Object message);

}
