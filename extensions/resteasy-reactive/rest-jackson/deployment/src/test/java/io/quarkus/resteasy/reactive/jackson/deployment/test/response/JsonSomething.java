package io.quarkus.resteasy.reactive.jackson.deployment.test.response;

public class JsonSomething {
    public String firstName;
    public String lastName;

    public JsonSomething(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
