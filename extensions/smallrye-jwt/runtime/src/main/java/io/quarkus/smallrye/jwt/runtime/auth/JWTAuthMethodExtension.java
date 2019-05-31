package io.quarkus.smallrye.jwt.runtime.auth;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * An extension that adds support for the MP-JWT custom authentication mechanism
 * Additionally, registers an Undertow handler that cleans up MP JWT principal
 */
public class JWTAuthMethodExtension implements ServletExtension {
    @Inject
    JWTAuthContextInfo info;
    private String authMechanism;

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    /**
     * This registers the JWTAuthMechanismFactory under the "MP-JWT" mechanism name
     *
     * @param deploymentInfo - the deployment to augment
     * @param servletContext - the ServletContext for the deployment
     */
    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        deploymentInfo.addAuthenticationMechanism(authMechanism, new JWTAuthMechanismFactory(info));
    }
}
