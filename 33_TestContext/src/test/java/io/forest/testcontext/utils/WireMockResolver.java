package io.forest.testcontext.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.jetty12.Jetty12HttpServer;
import org.junit.jupiter.api.extension.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class WireMockResolver implements ParameterResolver, BeforeAllCallback, AfterEachCallback, AfterAllCallback {

    private static final WireMockServer WIREMOCK = new WireMockServer(
        wireMockConfig()
            // .httpServerFactory(new DirectCallHttpServerFactory())
            .httpServerFactory(Jetty12HttpServer::new)
            .usingFilesUnderClasspath("wiremock") // enables __files/ + mappings/
            .port(8080)
    );

    private static final WireMock CLIENTS;

    static {
        WIREMOCK.start();
        CLIENTS = WireMock.create()
            .port(WIREMOCK.port())
            .build();
        Runtime.getRuntime().addShutdownHook(new Thread(WIREMOCK::stop));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.isAnnotated(WireMockStub.class) ||
            parameterContext.getParameter().getType() == WireMockServer.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {

        Class<?> type = parameterContext.getParameter().getType();

        if (type == WireMockServer.class) {
            return WIREMOCK;
        }

        if (type == WireMock.class) {
            return CLIENTS;
        }

        return null;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Optional.ofNullable(context.getRequiredTestClass())
            .map(it -> it.getAnnotation(WireMockStub.class))
            .ifPresent(stub -> {
                System.out.println("======================= %s".formatted(stub.value()));
                    try {
                        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                        Resource[] resources = resolver.getResources(stub.value());
                        for (Resource resource : resources) {
                            CLIENTS.loadMappingsFrom(resource.getFile());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            );

    }

    @Override
    public void afterEach(ExtensionContext ec) {
        WIREMOCK.resetAll();  // ← clean stubs, keep server running
    }

    @Override
    public void afterAll(ExtensionContext ec) {
        // Optional: stop only if you want fresh port per test class
        // WIREMOCK.stop();
    }
}
