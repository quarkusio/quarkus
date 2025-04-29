package io.quarkus.it.keycloak.model;

public class User {

    private final String userName;
    private final String tenantId;

    public User(String name) {
        this.userName = name;
        this.tenantId = null;
    }

    public User(String userName, String tenantId) {
        this.userName = userName;
        this.tenantId = tenantId;
    }

    public String getUserName() {
        return userName;
    }

    public String getTenantId() {
        return tenantId;
    }
}