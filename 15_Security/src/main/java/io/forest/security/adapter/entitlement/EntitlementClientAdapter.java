package io.forest.security.adapter.entitlement;

import io.forest.security.application.dto.EntitlementDTO;
import io.forest.security.application.port.out.EntitlementsAdapter;
import io.forest.security.common.ResponseHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class EntitlementClientAdapter implements EntitlementsAdapter {

    Map<String, List<EntitlementDTO>> entitlementMap = Map.of(
            "johnsmith", List.of(new EntitlementDTO("USER", "READ")), // johnsmith
            "peterlinch", List.of(new EntitlementDTO("OTHER", "READ")) // johnsmith
    );

    @Override
    public ResponseHandler<List<EntitlementDTO>> findEntitlementByUserId(String userId) {
        log.info("Find entitlement [userId={}]", userId);
        return new ResponseHandler.Builder<List<EntitlementDTO>>()
                .success(entitlementMap.getOrDefault(userId, Collections.emptyList()))
                .build();
    }
}
