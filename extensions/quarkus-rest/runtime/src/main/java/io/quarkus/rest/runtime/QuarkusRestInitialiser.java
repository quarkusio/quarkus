package io.quarkus.rest.runtime;

import io.quarkus.rest.runtime.core.QuarkusRestDeployment;

public interface QuarkusRestInitialiser {
    /**
     * This is where we stuff all generated static init calls we need to make.
     */
    public void init(QuarkusRestDeployment deployment);
}
