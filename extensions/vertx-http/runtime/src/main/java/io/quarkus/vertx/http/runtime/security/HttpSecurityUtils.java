package io.quarkus.vertx.http.runtime.security;

import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.vertx.ext.web.RoutingContext;

public final class HttpSecurityUtils {
    // keep in sync with QuarkusPermissionSecurityIdentityAugmentor
    public final static String ROUTING_CONTEXT_ATTRIBUTE = "quarkus.http.routing.context";
    static final String SECURITY_IDENTITIES_ATTRIBUTE = "io.quarkus.security.identities";
    static final String COMMON_NAME = "CN";
    private static final String AUTHENTICATION_FAILURE_KEY = "io.quarkus.vertx.http.runtime.security#authentication-failure";

    private HttpSecurityUtils() {

    }

    /**
     * Provides all the {@link SecurityIdentity} created by the inclusive authentication.
     *
     * @return null if {@link RoutingContext} is not available or {@link #getSecurityIdentities(RoutingContext)}
     * @see #getSecurityIdentities(RoutingContext)
     */
    public static Map<String, SecurityIdentity> getSecurityIdentities(SecurityIdentity identity) {
        var routingContext = getRoutingContextAttribute(identity);
        if (routingContext == null) {
            return null;
        }
        return getSecurityIdentities(routingContext);
    }

    /**
     * When inclusive authentication is enabled, we allow all authentication mechanisms to produce identity.
     * However, only the first identity (provided by applicable mechanism with the highest priority) is stored
     * in the CDI container. Therefore, we put all the identities into the RoutingContext.
     *
     * @return null if no identities were found or map with authentication mechanism key and security identity value
     */
    public static Map<String, SecurityIdentity> getSecurityIdentities(RoutingContext routingContext) {
        return routingContext.get(SECURITY_IDENTITIES_ATTRIBUTE);
    }

    public static AuthenticationRequest setRoutingContextAttribute(AuthenticationRequest request, RoutingContext context) {
        request.setAttribute(ROUTING_CONTEXT_ATTRIBUTE, context);
        return request;
    }

    public static RoutingContext getRoutingContextAttribute(AuthenticationRequest request) {
        return request.getAttribute(ROUTING_CONTEXT_ATTRIBUTE);
    }

    public static RoutingContext getRoutingContextAttribute(SecurityIdentity identity) {
        RoutingContext routingContext = identity.getAttribute(RoutingContext.class.getName());
        if (routingContext != null) {
            return routingContext;
        }
        return identity.getAttribute(ROUTING_CONTEXT_ATTRIBUTE);
    }

    public static RoutingContext getRoutingContextAttribute(Map<String, Object> authenticationRequestAttributes) {
        return (RoutingContext) authenticationRequestAttributes.get(ROUTING_CONTEXT_ATTRIBUTE);
    }

    public static String getCommonName(X500Principal principal) {
        return getRdnValue(principal, COMMON_NAME);
    }

    static String getRdnValue(X500Principal principal, String rdnType) {
        try {
            LdapName ldapDN = new LdapName(principal.getName());

            // Apparently for some RDN variations it might not produce correct results
            // Can be tuned as necessary.
            for (Rdn rdn : ldapDN.getRdns()) {
                if (rdnType.equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (InvalidNameException ex) {
            // Failing the augmentation process because of this exception seems unnecessary
            // The RDN my include some characters unexpected by the legacy LdapName API specification.
        }
        return null;
    }

    /**
     * Adds {@link AuthenticationFailedException} failure to the current {@link RoutingContext}.
     * Main motivation is to have {@link AuthenticationFailedException#getAttributes()} available during challenge.
     */
    public static void addAuthenticationFailureToEvent(AuthenticationFailedException exception, RoutingContext routingContext) {
        if (routingContext != null && exception != null) {
            routingContext.put(AUTHENTICATION_FAILURE_KEY, exception);
        }
    }

    public static AuthenticationFailedException getAuthenticationFailureFromEvent(RoutingContext routingContext) {
        if (routingContext != null) {
            return routingContext.get(AUTHENTICATION_FAILURE_KEY);
        }
        return null;
    }

    public static VertxHttpConfig getDefaultVertxHttpConfig() {
        return new SmallRyeConfigBuilder()
                .addDiscoveredConverters()
                .withDefaultValue("quarkus.http.host", "8081")
                .withMapping(VertxHttpConfig.class)
                .build()
                .getConfigMapping(VertxHttpConfig.class);
    }
}
