package io.forest.testcontext.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@BeforeAll
@ExtendWith(WireMockResolver.class)
public @interface WireMockStub {
    String value();
}
