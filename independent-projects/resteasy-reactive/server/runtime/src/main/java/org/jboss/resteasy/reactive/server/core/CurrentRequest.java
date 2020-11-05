package org.jboss.resteasy.reactive.server.core;

public class CurrentRequest {

    static final ThreadLocal<ResteasyReactiveRequestContext> INSTANCE = new ThreadLocal<>();

    public static ResteasyReactiveRequestContext get() {
        return INSTANCE.get();
    }

    public static void set(ResteasyReactiveRequestContext set) {
        INSTANCE.set(set);
    }
}
