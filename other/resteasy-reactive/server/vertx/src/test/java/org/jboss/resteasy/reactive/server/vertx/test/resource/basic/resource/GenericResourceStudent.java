package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

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
