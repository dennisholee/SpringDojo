package io.forest.testcontext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
class PartyServiceTest {


    @Autowired
    PartyService partyService;

    @Test
    void test() {
        assertThat(partyService.findById(UUID.randomUUID()), nullValue());
    }
}