package io.forest.openapi.adapter.web;

import io.forest.openapi.adapter.api.server.UsersApiMockServer;
import io.forest.openapi.common.ResponseStatus;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@CustomLog
public class UsersApiTest {

    @LocalServerPort
    int port;

    @Test
    void test() {

        log.info(ResponseStatus.TEST, "UsersApiTest");
        log.info("What la");
        log.info("What's up {}", "doc");

        stubFor(
                UsersApiMockServer.stubQueryUserProfile200(
                            UsersApiMockServer.queryUserProfileRequestSample1(),
                            UsersApiMockServer.queryUserProfile200ResponseSample1()
                    )
        );

        String requestBody = "{ \"firstName\" : \"firstName\", \"lastName\" : \"lastName\", \"queryType\" : \"queryType\" }";

        given()
                .port(port)
                .auth()
                .basic("adam", "password")
                .basePath("/api")
                 .accept(MediaType.APPLICATION_JSON_VALUE)
                // .header("Accept", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
        .when()
                .post("/Users")
        .then()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .statusCode(HttpStatus.OK.value());


    }

    enum Foo {
        apple,
        banana
    }
}
