package org.jboss.shamrock.jwt.runtime.auth;

import javax.servlet.ServletContext;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * An extension that adds support for the MP-JWT custom authentication mechanism
 * Additionally, registers an Undertow handler that cleans up MP JWT principal
 */
public class JWTAuthMethodExtension implements ServletExtension {
    private final String authMechanism;
    private final JWTAuthContextInfo contextInfo;

    public JWTAuthMethodExtension(String authMechanism, JWTAuthContextInfo contextInfo) {
        this.authMechanism = authMechanism;
        this.contextInfo = contextInfo;
    }

    public String getAuthMechanism() {
        return authMechanism;
    }

    public JWTAuthContextInfo getContextInfo() {
        return contextInfo;
    }

    /**
     * This registers the JWTAuthMechanismFactory under the "MP-JWT" mechanism name
     *
     * @param deploymentInfo - the deployment to augment
     * @param servletContext - the ServletContext for the deployment
     */
    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        deploymentInfo.addAuthenticationMechanism(authMechanism, new JWTAuthMechanismFactory(contextInfo));
        deploymentInfo.addInnerHandlerChainWrapper(MpJwtPrincipalCleanupHandler::new);
    }
}
