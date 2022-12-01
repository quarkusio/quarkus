package io.quarkus.rest.client.reactive.registerclientheaders;

public final class ComputedHeader {

    public static String get() {
        return "From " + ComputedHeader.class.getName();
    }

}
