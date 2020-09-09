package io.quarkus.cli.core;

import java.nio.file.Path;
import java.util.List;

import io.quarkus.devtools.project.BuildTool;

/**
 * A buildsystem command that can only be run solo
 */
public interface BuildsystemCommand {
    /**
     * Should this buildsystem command be run within one build process?
     *
     * @param buildtool
     * @return
     */
    default boolean aggregate(BuildTool buildtool) {
        return false;
    }

    /**
     * If aggregate() then this method should be called.
     *
     * @param projectDir
     * @param buildtool
     * @return list of arguments to pass to build process
     */
    default List<String> getArguments(Path projectDir, BuildTool buildtool) {
        throw new IllegalStateException("Command implemented incorrectly");
    }

    /**
     * This command is responsible for executing buildsystem if aggregate() is false
     *
     * @param projectDir
     * @param buildtool
     * @return
     */
    default int execute(Path projectDir, BuildTool buildtool) throws Exception {
        throw new IllegalStateException("Command implemented incorrectly");
    }
}
