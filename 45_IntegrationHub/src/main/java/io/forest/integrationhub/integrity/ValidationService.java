package io.forest.integrationhub.integrity;

import com.google.protobuf.Message;

/**
 * Simple validation utilities for incoming messages.
 * This is intentionally lightweight; add schema-specific checks as needed.
 */
public final class ValidationService {

    private ValidationService() {
    }

    /**
     * Validate a protobuf message. Throws ValidationException for invalid inputs.
     */
    public static void validateProto(Message message) {
        if (message == null) {
            throw new ValidationException("message is null");
        }

        if (message.getSerializedSize() == 0) {
            throw new ValidationException("message appears empty");
        }

        // Small example of a schema-specific check for the demo EchoRequest proto.
        // Keep this optional and minimal — real schema validation should be richer.
        if (message instanceof io.forest.integrationhub.v1.EchoRequest) {
            io.forest.integrationhub.v1.EchoRequest req = (io.forest.integrationhub.v1.EchoRequest) message;
            if (req.getMessage() == null || req.getMessage().isEmpty()) {
                throw new ValidationException("EchoRequest.message must be set");
            }
        }
    }
}
