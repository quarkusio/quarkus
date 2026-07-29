package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public class JsonAliasSameAsFieldNameBean {

    private String name;

    @JsonAlias({ "documentId", "id" })
    private UUID id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
