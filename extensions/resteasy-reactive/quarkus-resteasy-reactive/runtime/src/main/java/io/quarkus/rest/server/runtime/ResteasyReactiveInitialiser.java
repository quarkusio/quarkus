package io.quarkus.rest.server.runtime;

import org.jboss.resteasy.reactive.server.core.Deployment;

public interface ResteasyReactiveInitialiser {
    /**
     * This is where we stuff all generated static init calls we need to make.
     */
    void init(Deployment deployment);
}
