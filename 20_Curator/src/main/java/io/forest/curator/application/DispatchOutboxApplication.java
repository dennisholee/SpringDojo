package io.forest.curator.application;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

import java.util.List;

import io.forest.curator.application.command.DispatchOutboxCommand;
import io.forest.curator.common.model.Status;
import io.forest.curator.port.DispatchOutbox;
import io.forest.curator.port.MessageGateway;
import io.forest.curator.port.OutboxRepository;
import io.forest.curator.port.dto.OutboxDto;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class DispatchOutboxApplication implements DispatchOutbox {

	@NonNull
	OutboxRepository taskRepository;

	@NonNull
	MessageGateway messageGateway;

	@Override
	@Transactional(value = REQUIRES_NEW)
	public void handleCommand(DispatchOutboxCommand command) {
		log.info("Received dispatch outbox command [cmd={}]", command);

		List<OutboxDto> dtoList = this.taskRepository.findByStatus(Status.WAIT);

		log.info("Proceeding to enqueue outbox message [size={}]", dtoList.size());

		for (OutboxDto outboxDto : dtoList) {
			this.messageGateway.enqueue(outboxDto);
			outboxDto.setStatus(Status.SENT);
			taskRepository.save(outboxDto);
		}

		dtoList.stream()
				.map(it -> {
					log.info("Processing outbox record [record={}]", it);
					return it;
				});
//				.map(messageGateway::enqueue)
//				.filter(Objects::nonNull)
//				.map(TaskDTO.class::cast)
//				.map(taskRepository::save);
	}
}