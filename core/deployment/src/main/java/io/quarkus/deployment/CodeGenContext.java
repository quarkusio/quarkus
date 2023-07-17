package io.quarkus.deployment;

import java.nio.file.Path;

import org.eclipse.microprofile.config.Config;

import io.quarkus.bootstrap.model.ApplicationModel;

/**
 * Code generation context
 */
public class CodeGenContext {
    private final ApplicationModel model;
    private final Path outDir;
    private final Path workDir;
    private final Path inputDir;
    private final boolean redirectIO;
    private final Config config;
    private final boolean test;

    /**
     * Creates a code generation context
     *
     * @param model application model
     * @param outDir target directory for the generated output
     * @param workDir working directory, typically the main build directory of the project
     * @param inputDir directory containing input content for a code generator
     * @param redirectIO whether the code generating process should redirect its IO
     * @param config application build time configuration
     * @param test indicates whether the code generation is being triggered for tests
     */
    public CodeGenContext(ApplicationModel model, Path outDir, Path workDir, Path inputDir, boolean redirectIO,
            Config config, boolean test) {
        this.model = model;
        this.outDir = outDir;
        this.workDir = workDir;
        this.inputDir = inputDir;
        this.redirectIO = redirectIO;
        this.config = config;
        this.test = test;
    }

    /**
     * Application model
     *
     * @return application model
     */
    public ApplicationModel applicationModel() {
        return model;
    }

    /**
     * Target directory for the generated output.
     * The directory would typically be resolved as {@code <project.build.directory>/generated-sources/<codegen-provider-id>},
     * where {@code <codegen-provider-id> would match the value of {@link CodeGenProvider#providerId()}.
     * For example, for a code gen provider {@code foo}, the output directory in a typical Maven project would be
     * {@code target/generated-sources/foo}.
     *
     * @return target directory for the generated output
     */
    public Path outDir() {
        return outDir;
    }

    /**
     * Working directory, typically the main build directory of the project.
     * For a typical Maven project it would be the {@code target} directory.
     *
     * @return working directory, typically the main build directory of the project
     */
    public Path workDir() {
        return workDir;
    }

    /**
     * Directory containing input content for a code generator.
     * For the main application build of a typical Maven project the input sources directory
     * would be {@code <project.basedir>/src/main/<codegen-provider-id>}, while for the tests it would be
     * {@code <project.basedir>/src/test/<codegen-provider-id>}, where {@code <codegen-provider-id}
     * would match the value of {@link CodeGenProvider#providerId()}.
     *
     * @return directory containing input content a code generator
     */
    public Path inputDir() {
        return inputDir;
    }

    /**
     * Whether any new processes spawned by a given {@link CodeGenProvider} should inherit the
     * launching process' output streams
     * or redirect its output and error streams using {@link java.lang.ProcessBuilder.Redirect#PIPE}.
     * In the current implementation this is typically set to {@code true} by the framework.
     *
     * @return whether the code generation process should redirect its error and output streams
     */
    public boolean shouldRedirectIO() {
        return redirectIO;
    }

    /**
     * Application build time configuration
     *
     * @return application build time configuration
     */
    public Config config() {
        return config;
    }

    /**
     * Indicates whether the code generation is being triggered for tests
     *
     * @return indicates whether the code generation is being triggered for tests
     */
    public boolean test() {
        return test;
    }
}
