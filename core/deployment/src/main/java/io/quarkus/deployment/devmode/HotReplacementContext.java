package io.quarkus.deployment.devmode;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;

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
    boolean doScan(boolean userInitiated) throws Exception;

    void addPreScanStep(Runnable runnable);

    /**
     * The consumer is invoked if only files which don't require restart are modified.
     * 
     * @param consumer The input is a set of chaned file paths
     * @see HotDeploymentWatchedFileBuildItem#isRestartNeeded()
     */
    void consumeNoRestartChanges(Consumer<Set<String>> consumer);
}
