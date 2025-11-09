package io.forest.testcontext;

import io.forest.testcontext.utils.SuperTest;
import io.forest.testcontext.utils.WireMockStub;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;


@SuperTest
@WireMockStub("wiremock/address")
//@Execution(ExecutionMode.CONCURRENT)
class PartyServiceNestedTest {

    @ActiveProfiles("testA")
    @Nested
    class NestTestA {

        @Autowired
        PartyService partyServiceA;

        @Test
        void test() {
            assertThat(partyServiceA.findById(UUID.randomUUID()), nullValue());
        }
    }

    @ActiveProfiles("testB")
    @Nested
    class NestTestB {
        @Autowired
        PartyService partyServiceB;

        @Test
        void test() {
            assertThat(partyServiceB.findById(UUID.randomUUID()), nullValue());
        }
    }
}