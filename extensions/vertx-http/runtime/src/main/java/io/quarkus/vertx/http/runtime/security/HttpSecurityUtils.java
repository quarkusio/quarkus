package io.quarkus.vertx.http.runtime.security;

import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import io.quarkus.security.identity.request.AuthenticationRequest;
import io.vertx.ext.web.RoutingContext;

public final class HttpSecurityUtils {
    public final static String ROUTING_CONTEXT_ATTRIBUTE = "quarkus.http.routing.context";
    static final String COMMON_NAME = "CN";

    private HttpSecurityUtils() {

    }

    public static AuthenticationRequest setRoutingContextAttribute(AuthenticationRequest request, RoutingContext context) {
        request.setAttribute(ROUTING_CONTEXT_ATTRIBUTE, context);
        return request;
    }

    public static RoutingContext getRoutingContextAttribute(AuthenticationRequest request) {
        return request.getAttribute(ROUTING_CONTEXT_ATTRIBUTE);
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
}
