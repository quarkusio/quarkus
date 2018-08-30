package org.jboss.shamrock.maven.runner;

import java.io.File;
import java.nio.file.Paths;

import javax.servlet.ServletContext;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

public class RuntimeCompilationServletExtension implements ServletExtension {

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        String classesDir = System.getProperty("shamrock.runner.classes");
        String sourcesDir = System.getProperty("shamrock.runner.sources");

        if (classesDir != null) {
            deploymentInfo.addInitialHandlerChainWrapper(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    ClassLoaderCompiler compiler = null;
                    try {
                        compiler = new ClassLoaderCompiler(deploymentInfo.getClassLoader(), new File(classesDir));
                    } catch (Exception e) {
                        servletContext.log("Failed to create compiler, runtime compilation will be unavailable", e);
                    }

                    return new RuntimeUpdatesHandler(handler, Paths.get(classesDir), Paths.get(sourcesDir), compiler);
                }
            });
        }
    }
}
