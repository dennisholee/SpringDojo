package io.forest.spring.kafka.unittest.adapter.foo;

import io.forest.spring.kafka.unittest.app.port.FooEventListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.mock.MockConsumerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@SpringJUnitConfig
@Import({AppTest.Config.class})
@DirtiesContext
class AppTest {

    @Autowired
    Config config;

    @Test
    void test() {
        assertAll(
            () -> assertTrue(this.config.receivedLatch.await(10, TimeUnit.SECONDS))
        );
    }


    @Configuration(proxyBeanMethods = false)
    @EnableKafka
    public static class Config {

        final List<ConsumerRecord<String, FooEvent>> consumerRecordList = List.of(
            new ConsumerRecord<>("topic", 0, 0L, "event001", FooEvent.of("Event001"))
        );

        final CountDownLatch receivedLatch = new CountDownLatch(consumerRecordList.size());

        @Bean
        FooEventListener fooEventListener() {
            return new FooEventListener(
                fooEvent -> {
                    System.out.printf(">>>> %s%n", fooEvent);
                    this.receivedLatch.countDown();
                }
            );
        }

        @Bean
        ConsumerFactory<String, FooEvent> consumerFactory() {
            Map<TopicPartition, Long> beginningOffsets = Stream.of(
                new TopicPartition("topic", 0)
            ).collect(
                Collectors.toMap(
                    Function.identity(),
                    topicPartition -> 0L
                )
            );

            try (MockConsumer<String, FooEvent> consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST){
                @Override
                public void close() {}
            }) {
                consumer.updateBeginningOffsets(beginningOffsets);
                consumer.schedulePollTask(() -> consumerRecordList.forEach(consumer::addRecord));
                return new MockConsumerFactory<>(() -> consumer);
            }
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<?,?> kafkaListenerContainerFactory(ConsumerFactory consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<?,?> factory = new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            factory.getContainerProperties().setIdleBetweenPolls(100);
            return factory;
        }
    }
}
