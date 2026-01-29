package io.forest.security.conf;

import io.forest.security.application.UsersApp;
import org.springframework.context.annotation.Bean;

public class AppConf {

    @Bean
    UsersApp userApp() {
        return new UsersApp();
    }
}
