package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonView;

public class User {

    @JsonView(Views.Private.class)
    public int id;

    @JsonView(Views.Public.class)
    public String name;

    public static User testUser() {
        User user = new User();
        user.id = 1;
        user.name = "test";
        return user;
    }
}
