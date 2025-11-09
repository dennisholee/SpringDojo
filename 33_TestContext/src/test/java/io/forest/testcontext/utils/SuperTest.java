package io.forest.testcontext.utils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ExtendWith({
    WireMockResolver.class,
    MockitoExtension.class,
    OutputCaptureExtension.class,
    AddressResolver.class
})
public @interface SuperTest {
}
