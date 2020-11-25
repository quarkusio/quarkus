package org.jboss.resteasy.reactive.server.core;

public class CurrentRequestManager {

    private static volatile CurrentRequest currentRequest = new DefaultCurrentRequest();

    public static ResteasyReactiveRequestContext get() {
        return currentRequest.get();
    }

    public static void set(ResteasyReactiveRequestContext set) {
        currentRequest.set(set);
    }

    public static void setCurrentRequestInstance(CurrentRequest currentRequestInstance) {
        currentRequest = currentRequestInstance;
    }

    private static class DefaultCurrentRequest implements CurrentRequest {
        static final ThreadLocal<ResteasyReactiveRequestContext> INSTANCE = new ThreadLocal<>();

        @Override
        public ResteasyReactiveRequestContext get() {
            return INSTANCE.get();
        }

        @Override
        public void set(ResteasyReactiveRequestContext set) {
            INSTANCE.set(set);
        }
    }
}
