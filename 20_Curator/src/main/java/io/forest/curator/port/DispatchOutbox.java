package io.forest.curator.port;

import io.forest.curator.application.command.DispatchOutboxCommand;

public interface DispatchOutbox {

	void handleCommand(DispatchOutboxCommand command);

}
