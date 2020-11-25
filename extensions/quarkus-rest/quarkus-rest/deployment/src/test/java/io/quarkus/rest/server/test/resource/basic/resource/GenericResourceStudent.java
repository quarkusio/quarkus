package io.quarkus.rest.server.test.resource.basic.resource;

public class GenericResourceStudent {
    private String name;

    public GenericResourceStudent() {
    }

    public GenericResourceStudent(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Student: " + name;
    }
}
