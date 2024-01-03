package io.quarkus.it.kafka.jsonschema;

public class Pet {

    private String name;
    private String color;

    public Pet() {
    }

    public Pet(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
