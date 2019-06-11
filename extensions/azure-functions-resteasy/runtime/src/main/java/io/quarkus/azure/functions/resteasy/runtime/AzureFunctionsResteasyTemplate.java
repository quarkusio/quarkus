package io.quarkus.azure.functions.resteasy.runtime;

import org.jboss.resteasy.spi.ResteasyDeployment;

import io.quarkus.runtime.annotations.Template;

@Template
public class AzureFunctionsResteasyTemplate {
    public static ResteasyDeployment deployment;
    public static String rootPath;

    public void start(String path, ResteasyDeployment dep) {
        deployment = dep;
        deployment.start();
        rootPath = path;
    }
}
