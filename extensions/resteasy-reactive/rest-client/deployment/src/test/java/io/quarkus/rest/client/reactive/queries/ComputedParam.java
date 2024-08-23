package io.quarkus.rest.client.reactive.queries;

public class ComputedParam {

    public static String withParam(String name) {
        if ("first".equals(name)) {
            return "-11";
        } else if ("second".equals(name)) {
            return "-22";
        }
        throw new IllegalArgumentException();
    }
}
