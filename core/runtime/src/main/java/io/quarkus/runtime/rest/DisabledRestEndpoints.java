package io.quarkus.runtime.rest;

import java.util.List;
import java.util.Map;

/**
 * This class serves for passing a list of disabled REST paths (via the `@EndpointDisabled` annotation)
 * so that an OpenAPI filter can omit them from the generated OpenAPI document.
 */
public class DisabledRestEndpoints {

    // keys are REST paths, values are HTTP methods disabled on the given path
    private static Map<String, List<String>> endpoints;

    public static void set(Map<String, List<String>> endpoints) {
        DisabledRestEndpoints.endpoints = endpoints;
    }

    public static Map<String, List<String>> get() {
        return endpoints;
    }
}
