package org.jboss.resteasy.reactive.server.core;

import javax.ws.rs.core.Response;

public interface LazyResponse {

    /**
     * Gets the response, possibly generating it if it does not exist yet
     */
    Response get();

    /**
     *
     * @return <code>true</code> if the response already exists
     */
    boolean isCreated();

    /**
     * If {@code true}, it means that the Response is static and its data has been pre-populated and known in the request
     * context.
     * Otherwise, the build response could contain data that is not known in the request context and therefore needs to be
     * consulted.
     */
    default boolean isPredetermined() {
        return true;
    }

    class Existing implements LazyResponse {
        final Response response;

        public Existing(Response response) {
            this.response = response;
        }

        @Override
        public Response get() {
            return response;
        }

        @Override
        public boolean isCreated() {
            return true;
        }
    }
}
