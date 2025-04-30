package io.quarkus.smallrye.graphql.client.deployment.model;

import org.eclipse.microprofile.graphql.Input;

@Input("PersonInput")
public class PersonDto {
    private String name;

    public PersonDto() {
    }

    public PersonDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
