package org.jboss.resteasy.reactive.server.core.parameters.converters;

import org.jboss.resteasy.reactive.server.core.QuarkusRestDeployment;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;

public class ConverterInfo {

    private final QuarkusRestDeployment deployment;
    private final RuntimeResource resource;
    private final int param;

    public ConverterInfo(QuarkusRestDeployment deployment, RuntimeResource resource, int param) {
        this.deployment = deployment;
        this.resource = resource;
        this.param = param;
    }

    public QuarkusRestDeployment getDeployment() {
        return deployment;
    }

    public RuntimeResource getResource() {
        return resource;
    }

    public int getParam() {
        return param;
    }
}
