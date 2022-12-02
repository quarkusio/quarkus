package io.quarkus.it.kafka.codecs;

public class Person {

    private String name;

    private int id;

    public String getName() {
        return name;
    }

    public Person setName(String name) {
        this.name = name;
        return this;
    }

    public int getId() {
        return id;
    }

    public Person setId(int id) {
        this.id = id;
        return this;
    }
}
