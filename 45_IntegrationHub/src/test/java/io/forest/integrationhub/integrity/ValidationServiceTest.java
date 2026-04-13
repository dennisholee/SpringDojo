package io.forest.integrationhub.integrity;

import io.forest.integrationhub.v1.EchoRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationServiceTest {

    @Test
    void validEchoRequest_passesValidation() {
        EchoRequest req = EchoRequest.newBuilder().setMessage("hi").build();
        ValidationService.validateProto(req);
    }

    @Test
    void emptyEchoRequest_throwsValidationException() {
        EchoRequest req = EchoRequest.newBuilder().setMessage("").build();
        assertThrows(ValidationException.class, () -> ValidationService.validateProto(req));
    }
}
