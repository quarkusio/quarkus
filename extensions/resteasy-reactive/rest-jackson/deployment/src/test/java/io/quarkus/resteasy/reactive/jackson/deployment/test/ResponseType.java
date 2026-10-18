package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public enum ResponseType {
    /**
     * Returns DTOs directly.
     */
    PLAIN(true, "plain"),
    /**
     * Returns {@link Multi} with DTOs.
     */
    // TODO: enable when https://github.com/quarkusio/quarkus/issues/40447 gets fixed
    //MULTI(true, "multi"),
    /**
     * Returns {@link Uni} with DTOs.
     */
    UNI(true, "uni"),
    /**
     * Returns {@link Object} that is either DTO with a {@link SecureField} or not.
     */
    OBJECT(false, "object"), // we must always assume it can contain SecureField
    /**
     * Returns {@link Response} that is either DTO with a {@link SecureField} or not.
     */
    RESPONSE(false, "response"), // we must always assume it can contain SecureField
    /**
     * Returns {@link RestResponse} with DTOs.
     */
    REST_RESPONSE(true, "rest-response"),
    /**
     * Returns {@link RestResponse} with DTOs.
     */
    COLLECTION(true, "collection");

    private final boolean secureFieldDetectable;
    private final String resourceSubPath;

    ResponseType(boolean secureFieldDetectable, String resourceSubPath) {
        this.secureFieldDetectable = secureFieldDetectable;
        this.resourceSubPath = resourceSubPath;
    }

    boolean isSecureFieldDetectable() {
        return secureFieldDetectable;
    }

    String getResourceSubPath() {
        return resourceSubPath;
    }
}
