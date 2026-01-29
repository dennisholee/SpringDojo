package io.forest.security.adapter.web;

import io.forest.security.adapter.web.server.UsersApi;
import io.forest.security.adapter.web.server.model.QueryByName;
import io.forest.security.adapter.web.server.model.QueryUserResponse;
import io.forest.security.adapter.web.server.model.QueryUserResult;
import io.forest.security.application.UsersApp;
import io.forest.security.application.command.QueryByNameCommand;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.function.Function;

@RestController
@RequiredArgsConstructor
public class UsersRestApiController implements UsersApi {

    @NonNull
    UsersApp usersApp;

    @Override
    public ResponseEntity<QueryUserResponse> queryUserProfile(QueryByName queryByName) {

        return Optional.of(queryByName)
                .map(toQueryByNameCommand)
                .map(usersApp::handle)
                .orElseThrow()
                .ifOkOrElse(
                        it -> Optional.of(it)
                                .map(String.class::cast)
                                .map(s ->  new QueryUserResponse().user(new QueryUserResult().email(s)))
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build()),
                        (status, throwable) -> switch(status) {
                            case NOT_FOUND -> ResponseEntity.notFound().build();
                            case OK -> throw new RuntimeException("");
                            default -> ResponseEntity.internalServerError().build();
                        }
                );
    }

    Function<QueryByName, QueryByNameCommand> toQueryByNameCommand = q
            -> new QueryByNameCommand(q.getQueryType(), q.getFirstName(), q.getLastName());
}
