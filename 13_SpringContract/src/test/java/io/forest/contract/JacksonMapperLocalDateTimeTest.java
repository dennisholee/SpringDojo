package io.forest.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.forest.contract.conf.WebClientConf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(classes = WebClientConf.class)
public class JacksonMapperLocalDateTimeTest {

    @Autowired
    JavaTimeModule javaTimeModule;

    @Test
    void test() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(javaTimeModule);

        LocalDateTime localDateTime = objectMapper.readValue("202501312359", LocalDateTime.class);

        assertAll(
                () -> assertThat(objectMapper.readValue("202501312359", LocalDateTime.class),
                        equalTo(LocalDateTime.of(2025,1,31,23,59)))
        );
    }
}
