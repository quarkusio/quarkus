package io.quarkus.it.rest;

import java.time.LocalDate;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

public class Greeting {

    private final String message;
    private final LocalDate date;

    @JsonbCreator
    public Greeting(@JsonbProperty("message") String message, @JsonbProperty("date") LocalDate date) {
        this.message = message;
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public LocalDate getDate() {
        return date;
    }
}
