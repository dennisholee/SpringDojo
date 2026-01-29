package io.forest.security.application.command;

public record QueryByNameCommand(String queryType, String firstName, String lastName) {

}
