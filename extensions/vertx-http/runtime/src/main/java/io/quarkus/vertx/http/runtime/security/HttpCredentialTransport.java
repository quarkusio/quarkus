package io.quarkus.vertx.http.runtime.security;

import java.util.Objects;

/**
 * A representation of HTTP credential transport. In particular this includes things such as:
 *
 * Cookies
 * Authorization header
 * POST
 *
 * Note that using multiple HTTP authentication mechanisms to use the same credential
 * transport type can lead to unexpected authentication failures as they will not be able to figure out which mechanisms should
 * process which
 * request.
 */
public class HttpCredentialTransport {

    private final Type transportType;
    private final String typeTarget;
    private final String authenticationScheme;

    /**
     * HttpCredentialTransport constructor that accepts two parameters.
     *
     * @param transportType the transport type, for example, {@link Type#AUTHORIZATION}
     * @param typeTarget the type target, case-insensitive, for example, "Bearer".
     *        It also becomes an authentication scheme returned from {@link HttpCredentialTransport#getAuthenticationScheme()}
     */
    public HttpCredentialTransport(Type transportType, String typeTarget) {
        this(transportType, typeTarget, typeTarget);
    }

    /**
     * HttpCredentialTransport constructor that accepts three parameters.
     *
     * @param transportType the transport type, for example, {@link Type#AUTHORIZATION}
     * @param typeTarget the type target, case-insensitive, for example, "Bearer" or a Form authentication post location.
     * @param authenticationScheme case-insensitive. Usually, it is equal to the {@link #typeTarget} but can also take a unique
     *        value.
     *        For example, "Form" for a Form authentication mechanism whose {@link #typeTarget} may be set to a form post
     *        location.
     *        <p>
     *        Both {@link io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism#value()}
     *        and an optional HTTP security policy's `auth-mechanism` property must be set to the "authenticationScheme" value
     *        to find a matching {@link HttpAuthenticationMechanism} that must secure a specific REST resource method or request
     *        path.
     *        For example, '@HttpAuthenticationMechanism("custom-scheme")',
     *        'quarkus.http.auth.permission.my-policy.auth-mechanism=custom-scheme'.
     */
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
        X509,
        /**
         * Authorization code, type target is the query 'code' parameter
         */
        AUTHORIZATION_CODE,
        /**
         * Reserved for HTTP credential transport used during the security testing
         * with the 'io.quarkus.test.security.TestSecurity' annotation.
         */
        TEST_SECURITY
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
