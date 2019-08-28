package io.quarkus.elytron.security.runtime;

import javax.servlet.ServletContext;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

public class SecurityContextPrincipalExtension implements ServletExtension {
    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        deploymentInfo.addInnerHandlerChainWrapper(SecurityContextPrincipalHandler::new);
    }
}
