package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.validation.constraints.NotBlank;

public class Person {

    private String first;

    @NotBlank(message = "Title cannot be blank")
    private String last;

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }
}
