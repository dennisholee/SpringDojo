package io.forest.contract.conf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static io.forest.contract.commons.UncheckSupplier.unchecked;

@Configuration
public class WebClientConf {

//    @Bean
//    ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder, JavaTimeModule javaTimeModule) {
//        return builder.build().registerModule(javaTimeModule);
//    }
    @Bean
    JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        List.of(
                yyyyMMddHHmmToLocalDateTime//, epochMilliToLocalDateTime
        ).forEach(
                func ->
                        module.addDeserializer(
                                LocalDateTime.class,
                                new JsonDeserializer<LocalDateTime>() {
                                    @Override
                                    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                                        return func.apply(p, ctxt);
                                    }
                                }
                        )

        );
        return module;
    }

//    BiFunction<JsonParser, DeserializationContext, LocalDateTime> epochMilliToLocalDateTime = (p, ctxt)
//            -> unchecked(() -> Instant.ofEpochMilli(p.readValueAs(Long.class)).atZone(ZoneId.systemDefault()).toLocalDateTime());

    BiFunction<JsonParser, DeserializationContext, LocalDateTime> yyyyMMddHHmmToLocalDateTime = (p, ctxt)
            -> unchecked(() -> LocalDateTime.parse(p.getValueAsString(), DateTimeFormatter.ofPattern("yyyyMMddHHmm")));
}
