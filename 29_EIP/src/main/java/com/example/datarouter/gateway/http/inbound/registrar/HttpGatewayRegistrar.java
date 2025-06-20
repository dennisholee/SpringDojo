package com.example.datarouter.gateway.http.inbound.registrar;

import com.example.datarouter.gateway.http.inbound.DefaultHttpGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.http.inbound.IntegrationRequestMappingHandlerMapping;
import org.springframework.messaging.Message;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpGatewayRegistrar {

    private final IntegrationRequestMappingHandlerMapping handlerMapping;


    public void registerGateway(DefaultHttpGateway gateway) {
        try {
            Method handleMethod = DefaultHttpGateway.class.getMethod("handleRequest", Object.class);

            RequestMappingInfo mappingInfo = RequestMappingInfo
                .paths(gateway.getPath())
                .methods(RequestMethod.valueOf(gateway.getMethod().toUpperCase()))
                .build();

            handlerMapping.registerMapping(
                mappingInfo,
                gateway,
                handleMethod
            );

            log.info("Registered HTTP gateway endpoint: {} {}", gateway.getMethod(), gateway.getPath());

        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to register HTTP gateway endpoint", e);
        }
    }
}
