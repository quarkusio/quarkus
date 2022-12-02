package io.quarkus.it.keycloak.model;

public class User {

    private final String userName;

    public User(String name) {
        this.userName = name;
    }

    public String getUserName() {
        return userName;
    }
}