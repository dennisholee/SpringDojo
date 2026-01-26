package io.forest.dremio;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner runner(ChatClient.Builder builder, JdbcTemplate jdbcTemplate) {

        ChatClient chatClient = builder.build();

        return args -> {

            List<Map<String, @Nullable Object>> data = jdbcTemplate.queryForList("SELECT * FROM Dev01.demo01.Book_View");


            String content = chatClient.prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(u -> u.text("Analyze this data: {data}. Question: {query}")
                    .param("data", data.toString())
                    .param("query", "List books written by Vince"))
                .call()
                .content();

            System.out.println(content);
        };
    }
}
