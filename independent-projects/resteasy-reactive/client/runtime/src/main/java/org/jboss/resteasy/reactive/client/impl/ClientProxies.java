package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.ext.ParamConverterProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.jboss.resteasy.reactive.client.api.InvalidRestClientDefinitionException;

public class ClientProxies {

    final Map<Class<?>, BiFunction<WebTarget, List<ParamConverterProvider>, ?>> clientProxies;
    private final Map<Class<?>, String> failures;

    public ClientProxies(Map<Class<?>, BiFunction<WebTarget, List<ParamConverterProvider>, ?>> clientProxies,
            Map<Class<?>, String> failures) {
        this.clientProxies = clientProxies;
        this.failures = failures;
    }

    public <T> T get(Class<?> clazz, WebTarget webTarget, List<ParamConverterProvider> providers) {
        BiFunction<WebTarget, List<ParamConverterProvider>, ?> function = clientProxies.get(clazz);
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
        return (T) function.apply(webTarget, providers);
    }

    // for dev console
    public ClientData getClientData() {
        return new ClientData(clientProxies.keySet(), failures);
    }

    public static class ClientData {
        public final Collection<Class<?>> clientClasses;
        public final Map<Class<?>, String> failures;

        public ClientData(Collection<Class<?>> clientClasses, Map<Class<?>, String> failures) {
            this.clientClasses = clientClasses;
            this.failures = failures;
        }
    }
}
