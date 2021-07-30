package io.quarkus.vertx.http.runtime.security;

import java.util.Objects;

/**
 * A representation of HTTP credential transport. In particular this includes things such as:
 * 
 * Cookies
 * Authorization header
 * POST
 * 
 * It is not permitted for multiple HTTP authentication mechanisms to use the same credential
 * transport type, as they will not be able to figure out which mechanisms should process which
 * request.
 */
public class HttpCredentialTransport {

    private final Type transportType;
    private final String typeTarget;
    private final String authenticationScheme;

    public HttpCredentialTransport(Type transportType, String typeTarget) {
        this(transportType, typeTarget, typeTarget);
    }

    public HttpCredentialTransport(Type transportType, String typeTarget, String authenticationScheme) {
        this.transportType = Objects.requireNonNull(transportType);
        this.typeTarget = Objects.requireNonNull(typeTarget).toLowerCase();
        this.authenticationScheme = Objects.requireNonNull(authenticationScheme).toLowerCase();
    }

    public enum Type {
        /**
         * A cookie. The type target is the cookie name
         */
        COOKIE,
        /**
         * Auth header, type target is the auth type (basic, bearer etc)
         */
        AUTHORIZATION,
        /**
         * A different header, type target is the header name
         */
        OTHER_HEADER,
        /**
         * A post request, target is the POST URI
         */
        POST,
        /**
         * X509
         */
        X509
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        HttpCredentialTransport that = (HttpCredentialTransport) o;

        if (transportType != that.transportType)
            return false;
        return typeTarget.equals(that.typeTarget) && this.authenticationScheme.equals(that.authenticationScheme);
    }

    @Override
    public int hashCode() {
        int result = transportType.hashCode();
        result = 31 * result + typeTarget.hashCode();
        result = 31 * result + authenticationScheme.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "HttpCredentialTransport{" +
                "transportType=" + transportType +
                ", typeTarget='" + typeTarget + '\'' +
                ", authenticationScheme='" + authenticationScheme + '\'' +
                '}';
    }

    public Type getTransportType() {
        return transportType;
    }

    public String getTypeTarget() {
        return typeTarget;
    }

    public String getAuthenticationScheme() {
        return authenticationScheme;
    }
}
