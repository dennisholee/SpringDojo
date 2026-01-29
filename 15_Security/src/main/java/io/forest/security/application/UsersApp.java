package io.forest.security.application;

import io.forest.security.application.command.QueryByNameCommand;
import io.forest.security.common.ResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RequiredArgsConstructor
public class UsersApp {


    @PreAuthorize("hasRole('USER_READ')")
    public ResponseHandler<String> handle(QueryByNameCommand command) {
        return new ResponseHandler.Builder<String>()
                .success("Greetings!")
                .build();
    }
}
