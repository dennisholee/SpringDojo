package io.forest.caffeine.requestcoalesce.mapper;

import io.forest.caffeine.requestcoalesce.domain.User;
import io.forest.caffeine.requestcoalesce.model.UserData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "address", source = "address")
    User toUser(UserData userData);
} 