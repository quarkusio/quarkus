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
