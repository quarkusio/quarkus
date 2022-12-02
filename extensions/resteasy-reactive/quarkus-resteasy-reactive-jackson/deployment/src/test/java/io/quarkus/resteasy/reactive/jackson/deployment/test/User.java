package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonView;

public class User {

    @JsonView(Views.Private.class)
    public int id;

    @JsonView(Views.Public.class)
    public String name;
}
