package io.quarkus.it.resteasy.jsonb;

import javax.json.bind.annotation.JsonbNumberFormat;
import javax.json.bind.annotation.JsonbProperty;

public class Animal {

    @JsonbProperty(nillable = false)
    public final String color;
    private final int age;

    public Animal(String color, int age) {
        this.color = color;
        this.age = age;
    }

    @JsonbNumberFormat(value = "0.00")
    public int getAge() {
        return this.age;
    }
}
