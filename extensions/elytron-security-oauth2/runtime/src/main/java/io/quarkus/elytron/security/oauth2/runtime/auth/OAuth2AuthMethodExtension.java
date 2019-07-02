package io.quarkus.elytron.security.oauth2.runtime.auth;

import javax.servlet.ServletContext;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * An extension that adds support for the OAuth2 authentication mechanism
 * Additionally, registers an Undertow handler that cleans up OAuth2 principal
 */
public class OAuth2AuthMethodExtension implements ServletExtension {
    private String authMechanism;

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    /**
     * This registers the OAuth2AuthMechanismFactory under the "MP-JWT" mechanism name
     *
     * @param deploymentInfo - the deployment to augment
     * @param servletContext - the ServletContext for the deployment
     */
    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        deploymentInfo.addAuthenticationMechanism(authMechanism, new OAuth2AuthMechanismFactory());
    }
}
