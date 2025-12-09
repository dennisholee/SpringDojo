package io.forest.opa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.styra.opa.wasm.OpaPolicy;

import java.io.InputStream;
import java.security.PublicKey;
import java.util.Map;

public class Application {

    public static void main(String[] args) throws Exception {

        InputStream inputStream = Application.class.getClassLoader().getResourceAsStream("policy/policy.wasm");

        OpaPolicy opaPolicy = OpaPolicy.builder().withPolicy(inputStream).build();

        String user = "alice";
        String action = "read";
        String resource = "/data/sensitive";

        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInJvbGUiOiJ1c2VyIiwiaWF0IjoxNjcwMDQ2NDAwLCJleHAiOjE2NzAwNTAwMDB9.EXAMPLE_SIGNATURE_HERE";

        Map<String, String> inputMap = Map.of(
            "user", user,
            "action", action,
            "resource", resource,
            "token", token
        );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode inputNode = objectMapper.valueToTree(inputMap);


        String evaluated = opaPolicy.evaluate(inputNode);

        System.out.println(evaluated);


    }
}
