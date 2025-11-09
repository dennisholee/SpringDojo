package io.forest.testcontext;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.forest.testcontext.utils.AddressAnnotation;
import io.forest.testcontext.utils.AddressResolver;
import io.forest.testcontext.utils.SuperTest;
import io.forest.testcontext.utils.WireMockStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@SuperTest
@WireMockStub("wiremock/address")
class PartyServiceTest {

    @Autowired
    PartyService partyService;

    @Test
    void test(@AddressAnnotation Address address, CapturedOutput capturedOutput) {

        UUID partyId = UUID.fromString("7f569171-52c3-4355-8727-2d4a8fd8f68e");

        partyService.onboardParty(partyId, "John", "Smith");

        Party result = partyService.findById(partyId);

        System.out.println("result: %s".formatted(result));
        String output = capturedOutput.getOut();

        assertTrue(output.contains("Find by ID"));
    }
}