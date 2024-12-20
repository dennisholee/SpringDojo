package io.forest.openapi.common.security;

public interface Authorizer {
	
	default boolean hasPermission(String permission) {
		
		return false;
	}
}
