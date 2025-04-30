package io.quarkus.rest.client.reactive.headers;

import io.quarkus.rest.client.reactive.ComputedParamContext;

public class DummyHeaderCalculator {

    public static String calculate2(ComputedParamContext context) {
        return context.name();
    }
}
