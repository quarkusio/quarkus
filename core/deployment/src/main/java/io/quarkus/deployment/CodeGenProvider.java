package io.quarkus.deployment;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.microprofile.config.Config;
import org.wildfly.common.annotation.NotNull;

import io.quarkus.bootstrap.prebuild.CodeGenException;

/**
 * Service providers for this interface are triggered during generate-sources phase of build of Quarkus applications
 */
public interface CodeGenProvider {
    /**
     *
     * @return unique name of the code gen provider, will correspond to the directory in <code>generated-sources</code>
     */
    @NotNull
    String providerId();

    /**
     * File extension that CodeGenProvider will generate code from
     *
     * @return file extension
     */
    @NotNull
    String inputExtension();

    /**
     * Name of the directory containing input files for a given {@link CodeGenProvider} implementation
     * relative to a sources root directory. For example, if an input directory is configured as <code>foo</code>,
     * for a production build of an application the sources will be looked up at <code>src/main/foo</code> path
     * and at <code>src/test/foo</code> for tests.
     *
     * @return the input directory
     */
    @NotNull
    String inputDirectory();

    /**
     * Trigger code generation
     *
     * @param context code generation context
     * @return true if files were generated/modified
     */
    boolean trigger(CodeGenContext context) throws CodeGenException;

    default boolean shouldRun(Path sourceDir, Config config) {
        return Files.isDirectory(sourceDir);
    }
}
