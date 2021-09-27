package org.jboss.resteasy.reactive.client.impl;

import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.client.WebTarget;
import org.jboss.resteasy.reactive.client.api.InvalidRestClientDefinitionException;

public class ClientProxies {

    final Map<Class<?>, Function<WebTarget, ?>> clientProxies;
    private final Map<Class<?>, String> failures;

    public ClientProxies(Map<Class<?>, Function<WebTarget, ?>> clientProxies, Map<Class<?>, String> failures) {
        this.clientProxies = clientProxies;
        this.failures = failures;
    }

    public <T> T get(Class<?> clazz, WebTarget webTarget) {
        Function<WebTarget, ?> function = clientProxies.get(clazz);
        if (function == null) {
            String failure = failures.get(clazz);
            if (failure != null) {
                throw new InvalidRestClientDefinitionException(
                        "Failed to generate client for class " + clazz + " : " + failure);
            } else {
                throw new IllegalArgumentException("Not a REST client interface: " + clazz + ". No @Path annotation " +
                        "found on the class or any methods of the interface and no HTTP method annotations " +
                        "(@POST, @PUT, @GET, @HEAD, @DELETE, etc) found on any of the methods");
            }
        }
        //noinspection unchecked
        return (T) function.apply(webTarget);
    }
}
