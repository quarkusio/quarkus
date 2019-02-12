package org.jboss.shamrock.deployment.devmode;

import java.nio.file.Path;

public interface HotReplacementContext {

    Path getClassesDir();

    Path getSourcesDir();

    Path getResourcesDir();

    Throwable getDeploymentProblem();

    void doScan() throws Exception;
}
