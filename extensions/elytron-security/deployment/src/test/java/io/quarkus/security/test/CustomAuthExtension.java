package io.quarkus.security.test;

import javax.servlet.ServletContext;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * Undertow extension to register the CUSTOM auth method
 */
public class CustomAuthExtension implements ServletExtension {
    /**
     * This registers the CustomAuth under the "CUSTOM" mechanism name
     *
     * @param deploymentInfo - the deployment to augment
     * @param servletContext - the ServletContext for the deployment
     */
    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        deploymentInfo.addAuthenticationMechanism("CUSTOM", new CustomAuthFactory());
    }
}
