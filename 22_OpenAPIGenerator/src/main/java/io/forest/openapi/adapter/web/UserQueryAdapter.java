package io.forest.openapi.adapter.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import io.forest.openapi.adapter.api.server.UsersApiDelegate;
import io.forest.openapi.adapter.api.server.dto.QueryByName;
import io.forest.openapi.adapter.api.server.dto.QueryUserResponse;
import io.forest.openapi.adapter.api.server.dto.QueryUserResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserQueryAdapter implements UsersApiDelegate {

	@Override
	public ResponseEntity<QueryUserResponse> queryUserProfile(QueryByName queryByName) {
		QueryUserResponse response = new QueryUserResponse().user(new QueryUserResult().firstName("John")
				.lastName("doe")
				.membershipId(UUID.randomUUID()
						.toString())
				.email("john.doe@email.com"));
		return ResponseEntity.ok(response);
	}
}
