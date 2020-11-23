package org.jboss.resteasy.reactive.server.core;

public interface CurrentRequest {

    ResteasyReactiveRequestContext get();

    void set(ResteasyReactiveRequestContext set);
}
