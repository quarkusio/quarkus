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
     * Will return true if this is the remote side of a remote dev session
     * 
     * @return
     */
    DevModeType getDevModeType();

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
     * @param consumer The input is a set of changed file paths
     * @see HotDeploymentWatchedFileBuildItem#isRestartNeeded()
     */
    void consumeNoRestartChanges(Consumer<Set<String>> consumer);

    Set<String> syncState(Map<String, String> fileHashes);
}
