package io.forest.cdm.onboarding.internal.web;

import io.forest.cdm.shared.CdmDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The single translation point from domain failures to HTTP.
 *
 * <p>Handling the {@link CdmDomainException} base type rather than each concrete subclass means a
 * new domain failure is automatically translated without touching the web layer - and no domain
 * module ever has to know about status codes.
 *
 * <p>The exception is raised inside a transaction, so by the time this handler runs the engine has
 * already aborted the unit of work: no partial write is observable by the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CdmDomainException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> handleDomainFailure(CdmDomainException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.errorCode());
        body.put("message", ex.getMessage());
        return body;
    }
}
