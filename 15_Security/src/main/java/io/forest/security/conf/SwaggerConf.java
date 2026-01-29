package io.forest.security.conf;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;

public class SwaggerConf {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(
                new Info()
                        .title("Forest Security")
                        .description("Spring security demo.")
                        .version("v1.0")
        );
    }


//    @Bean
//    SpringDocConfiguration springDocConfiguration(){
//        return new SpringDocConfiguration();
//    }
//
//    @Bean
//    SpringDocConfigProperties springDocConfigProperties() {
//        return new SpringDocConfigProperties();
//    }
//
//    @Bean
//    ObjectMapperProvider objectMapperProvider(SpringDocConfigProperties springDocConfigProperties){
//        return new ObjectMapperProvider(springDocConfigProperties);
//    }

//    @Bean
//    SpringDocUIConfiguration SpringDocUIConfiguration(Optional<SwaggerUiConfigProperties> optionalSwaggerUiConfigProperties){
//        return new SpringDocUIConfiguration(optionalSwaggerUiConfigProperties);
//    }
}
