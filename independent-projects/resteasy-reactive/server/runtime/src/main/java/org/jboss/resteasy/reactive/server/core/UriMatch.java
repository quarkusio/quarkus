package org.jboss.resteasy.reactive.server.core;

import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;

public class UriMatch {
    public final String matched;
    public final RuntimeResource resource;
    public final Object target;

    public UriMatch(String matched, RuntimeResource resource, Object target) {
        this.matched = matched;
        this.resource = resource;
        this.target = target;
    }
}
