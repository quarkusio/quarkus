package io.quarkus.swaggerui.runtime;

import static io.undertow.Handlers.resource;

import java.io.File;

import javax.servlet.ServletContext;

import org.jboss.logging.Logger;

import io.undertow.Handlers;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

public class SwaggerUiServletExtension implements ServletExtension {

    private static final Logger log = Logger.getLogger(SwaggerUiServletExtension.class.getName());

    private String path;
    private String resourceDir;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getResourceDir() {
        return resourceDir;
    }

    public void setResourceDir(String resourceDir) {
        this.resourceDir = resourceDir;
    }

    /**
     * This registers the SwaggerUiServletExtension
     *
     * @param deploymentInfo - the deployment to augment
     * @param servletContext - the ServletContext for the deployment
     */
    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        ResourceManager resourceManager = new FileResourceManager(new File(resourceDir));
        deploymentInfo.addOuterHandlerChainWrapper(
                (handler) -> Handlers.path(handler).addPrefixPath(path, resource(resourceManager)));
        log.info("Swagger UI available at " + path);
    }

}
