package io.forest.security.application.port.out;

import io.forest.security.application.dto.EntitlementDTO;
import io.forest.security.common.ResponseHandler;

import java.util.List;

public interface EntitlementsAdapter {


    ResponseHandler<List<EntitlementDTO>> findEntitlementByUserId(String userId);
}
