package io.quarkus.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.config.Config;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;

/**
 * Service providers for this interface are triggered during generate-sources phase of build of Quarkus applications
 */
public interface CodeGenProvider {
    /**
     * @return unique name of the code gen provider, will correspond to the directory in <code>generated-sources</code>
     */
    String providerId();

    /**
     * File extension that CodeGenProvider will generate code from
     * Deprecated: use inputExtensions instead
     *
     * @return file extension
     */
    @Deprecated
    default String inputExtension() {
        return null;
    }

    /**
     * File extensions that CodeGenProvider will generate code from
     *
     * @return file extensions
     */
    default String[] inputExtensions() {
        if (inputExtension() != null) {
            return new String[] { inputExtension() };
        }
        return new String[] {};
    }

    /**
     * Name of the directory containing input files for a given {@link CodeGenProvider} implementation
     * relative to a sources root directory. For example, if an input directory is configured as <code>foo</code>,
     * for a production build of an application the sources will be looked up at <code>src/main/foo</code> path
     * and at <code>src/test/foo</code> for tests.
     *
     * @return the input directory
     */
    String inputDirectory();

    /**
     * Provides the possibility for the provider to override the default input directory.
     * This method is called after {@link #init(ApplicationModel, Map)}.
     * Returning {@code null} will result in the {@code inputDirectory} method being called to retrieve the default input
     * directory.
     * <p>
     * The returned path must be an absolute path. However, pointing to a directory outside of the project structure should
     * be avoided for security purposes.
     *
     * @return the input directory, must be an absolute path. {@code null} would result in the default input directory being
     *         used.
     */
    default Path getInputDirectory() {
        return null;
    }

    /**
     * Provides the possibility for the provider to initialize itself using the application model and properties.
     *
     * @param model the application model
     * @param properties the build time properties defined in the application build file (pom.xml or gradle.build)
     */
    default void init(ApplicationModel model, Map<String, String> properties) {
        // No-op
    }

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

    /**
     * Resolve path; e.g. symlinks, etc
     *
     * @param path the path to resolve
     * @return resolved path
     */
    static Path resolve(Path path) {
        return Objects.requireNonNull(path).resolve(".");
    }
}
