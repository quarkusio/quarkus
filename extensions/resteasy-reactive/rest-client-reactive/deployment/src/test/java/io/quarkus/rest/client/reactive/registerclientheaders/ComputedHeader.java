package io.quarkus.rest.client.reactive.registerclientheaders;

import io.quarkus.rest.client.reactive.ComputedParamContext;

public final class ComputedHeader {

    public static String get() {
        return "From " + ComputedHeader.class.getName();
    }

    public static String contentType(ComputedParamContext context) {
        return "application/json;param=" + context.methodParameters().get(1).value();
    }

}
