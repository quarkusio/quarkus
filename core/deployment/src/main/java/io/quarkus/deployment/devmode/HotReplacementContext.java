package io.quarkus.deployment.devmode;

import java.nio.file.Path;
import java.util.List;

public interface HotReplacementContext {

    Path getClassesDir();

    List<Path> getSourcesDir();

    List<Path> getResourcesDir();

    Throwable getDeploymentProblem();

    /**
     * 
     * @return {@code true} if a restart was performed, {@code false} otherwise
     * @throws Exception
     */
    boolean doScan() throws Exception;

    void addPreScanStep(Runnable runnable);
}
