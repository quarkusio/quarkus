package io.quarkus.rest.runtime.core.parameters.converters;

import io.quarkus.rest.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.runtime.mapping.RuntimeResource;

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
