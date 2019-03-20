package io.quarkus.deployment.devmode;

import java.nio.file.Path;

public interface HotReplacementContext {

    Path getClassesDir();

    Path getSourcesDir();

    Path getResourcesDir();

    Throwable getDeploymentProblem();

    /**
     * 
     * @return {@code true} if a restart was performed, {@code false} otherwise
     * @throws Exception
     */
    boolean doScan() throws Exception;
}
