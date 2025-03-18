package io.forest.spring.kafka.unittest.app.port;

import io.forest.spring.kafka.unittest.adapter.foo.FooEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class FooEventListener {

    @NonNull
    Consumer<FooEvent> consumer;

    @KafkaListener(
        topicPartitions = @TopicPartition(
            topic = "topic",
            partitions = "0"
        )
    )
    public void listener(@Payload FooEvent event, Acknowledgment acknowledgment) {
        consumer.accept(event);
    }
}
