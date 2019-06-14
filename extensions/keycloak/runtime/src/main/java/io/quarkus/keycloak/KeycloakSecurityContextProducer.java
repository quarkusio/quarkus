package io.quarkus.keycloak;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.keycloak.KeycloakSecurityContext;

/**
 * This class is responsible for producing {@link KeycloakSecurityContext} instances so that applications are able to inject
 * those instance into their beans in order to obtain information about the security context created by the Keycloak Extension.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakSecurityContextProducer {

    @Inject
    HttpServletRequest request;

    @Produces
    @RequestScoped
    public KeycloakSecurityContext produceSecurityContext() {
        return (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    }
}
