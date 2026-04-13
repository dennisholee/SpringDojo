package io.forest.integrationhub.integrity;

import io.forest.integrationhub.v1.EchoRequest;
import io.forest.integrationhub.v1.EchoResponse;
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

    @Test
    void nonEchoResponse_isIgnored() {
        EchoResponse resp = EchoResponse.newBuilder().setMessage("pong").build();
        // Should not throw for non-EchoRequest messages; original code ignores non-EchoRequest
        ValidationService.validateProto(resp);
    }
}
