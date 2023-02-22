package io.quarkus.it.resteasy.rest.client.reactive;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Greet {
    private final String greeting;
    private final String who;
    private final int number;

    public Greet(
            @JsonProperty("greeting") String greeting,
            @JsonProperty("who") String who,
            @JsonProperty("number") int number) {
        this.greeting = greeting;
        this.who = who;
        this.number = number;
    }

    @Pattern(regexp = "^[A-Z][a-z]+$")
    public String getGreeting() {
        return greeting;
    }

    @Pattern(regexp = "^[A-Z][a-z]+$")
    public String getWho() {
        return who;
    }

    @Min(1)
    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "Greet{" +
                "greeting='" + greeting + '\'' +
                ", who='" + who + '\'' +
                ", number=" + number +
                '}';
    }
}
