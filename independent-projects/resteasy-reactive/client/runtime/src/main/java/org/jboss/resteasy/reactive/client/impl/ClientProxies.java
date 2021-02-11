package org.jboss.resteasy.reactive.client.impl;

import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.client.WebTarget;

public class ClientProxies {

    final Map<Class<?>, Function<WebTarget, ?>> clientProxies;

    public ClientProxies(Map<Class<?>, Function<WebTarget, ?>> clientProxies) {
        this.clientProxies = clientProxies;
    }

    public <T> T get(Class<?> clazz, WebTarget webTarget) {
        Function<WebTarget, ?> function = clientProxies.get(clazz);
        if (function == null) {
            throw new IllegalArgumentException("Not a REST client interface: " + clazz);
        }
        return (T) function.apply(webTarget);
    }
}
