package io.forest.testcontext;

import io.forest.testcontext.resolver.AddressAnnotation;
import io.forest.testcontext.resolver.AddressResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@ExtendWith(AddressResolver.class)
class PartyServiceTest {


    @Autowired
    PartyService partyService;

    @Test
    void test(@AddressAnnotation Address address, CapturedOutput capturedOutput) {

        System.out.println(address);
        assertThat(partyService.findById(UUID.randomUUID()), nullValue());

        String output = capturedOutput.getOut();

        assertTrue(output.contains("Find by ID"));
    }
}