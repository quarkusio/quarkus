package io.quarkus.rest.client.reactive.runtime;

import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;

@SuppressWarnings("unused")
public final class ClientQueryParamSupport {

    private ClientQueryParamSupport() {
    }

    public static boolean isQueryParamPresent(WebTargetImpl webTarget, String name) {
        String query = webTarget.getUriBuilderUnsafe().getQuery();
        return query != null && query.contains(name + "=");
    }
}
