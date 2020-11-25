package io.quarkus.rest.server.test.resource.basic.resource;

public class DefaultMediaTypeCustomObject {
    public int a;
    public int b;

    public String toString() {
        return String.format("%d,%d", a, b);
    }

    public DefaultMediaTypeCustomObject(final int a, final int b) {
        this.a = a;
        this.b = b;
    }
}
