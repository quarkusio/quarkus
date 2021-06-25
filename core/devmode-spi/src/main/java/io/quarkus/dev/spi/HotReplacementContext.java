package io.quarkus.dev.spi;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface HotReplacementContext {

    Path getClassesDir();

    List<Path> getSourcesDir();

    List<Path> getResourcesDir();

    Throwable getDeploymentProblem();

    void setRemoteProblem(Throwable throwable);

    void updateFile(String file, byte[] data);

    /**
     * If this is true then this is a dev mode test case, rather than a user actually using Quarkus.
     *
     */
    boolean isTest();

    /**
     * Returns the type of the development mode
     * 
     * @return the dev mode type
     */
    DevModeType getDevModeType();

    /**
     * Scans for changed source files, and restarts if detected.
     *
     * @param userInitiated If this scan was initiated by a user action (e.g. refreshing a browser)
     * @return {@code true} if a restart was performed, {@code false} otherwise
     * @throws Exception
     */
    boolean doScan(boolean userInitiated) throws Exception;

    /**
     * Adds a task that is run before a live reload scan is performed.
     *
     * @param runnable The task to run
     */
    void addPreScanStep(Runnable runnable);

    /**
     * The consumer is invoked if only files which don't require restart are modified.
     *
     * @param consumer The input is a set of changed file paths
     * @see io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem#isRestartNeeded()
     */
    void consumeNoRestartChanges(Consumer<Set<String>> consumer);

    /**
     * This method returns a list of changed files compared to the provided map of file names to hashes.
     *
     * This is needed for remote dev mode, it is unlikely to be useful for anything else
     *
     * @param fileHashes The file hashes
     * @return A set of changed files
     */
    Set<String> syncState(Map<String, String> fileHashes);
}
