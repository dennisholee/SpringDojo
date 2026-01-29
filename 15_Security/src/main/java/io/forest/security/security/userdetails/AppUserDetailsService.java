package io.forest.security.security.userdetails;

import io.forest.security.application.dto.EntitlementDTO;
import io.forest.security.application.port.out.EntitlementsAdapter;
import io.forest.security.common.ResponseHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
public class AppUserDetailsService implements UserDetailsService {

    @NonNull
    EntitlementsAdapter entitlementsAdapter;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        AppUserDetails.Builder builder = new AppUserDetails.Builder(userId, null);

        ResponseHandler<List<EntitlementDTO>> responseHandler = entitlementsAdapter.findEntitlementByUserId(userId);

        assert Objects.nonNull(responseHandler.getPayload());

        log.info("Retrieved user's entitlements [userId={}, entitlementCount={}]", userId, responseHandler.getPayload().size());

        responseHandler.ifOkOrElse(
                it -> it.forEach(
                        dto -> builder.addAuthorities(dto.resource(), dto.permission())),
                (status, throwable) -> {
                }
        );

        return builder.build();
    }
}
