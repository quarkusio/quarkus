package io.quarkus.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.codegen.CodeGenData;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;

/**
 * A set of methods to initialize and execute {@link CodeGenProvider}s.
 */
public class CodeGenerator {

    // used by Gradle and Maven
    public static void initAndRun(ClassLoader classLoader,
            PathsCollection sourceParentDirs, Path generatedSourcesDir, Path buildDir,
            Consumer<Path> sourceRegistrar, ApplicationModel appModel, Properties properties,
            String launchMode) throws CodeGenException {
        List<CodeGenData> generators = init(classLoader, sourceParentDirs, generatedSourcesDir, buildDir, sourceRegistrar);
        for (CodeGenData generator : generators) {
            generator.setRedirectIO(true);
            trigger(classLoader, generator, appModel, properties, LaunchMode.valueOf(launchMode));
        }
    }

    public static List<CodeGenData> init(ClassLoader deploymentClassLoader,
            PathsCollection sourceParentDirs,
            Path generatedSourcesDir,
            Path buildDir,
            Consumer<Path> sourceRegistrar) throws CodeGenException {
        return callWithClassloader(deploymentClassLoader, () -> {
            List<CodeGenData> result = new ArrayList<>();
            Class<? extends CodeGenProvider> codeGenProviderClass;
            try {
                //noinspection unchecked
                codeGenProviderClass = (Class<? extends CodeGenProvider>) deploymentClassLoader
                        .loadClass(CodeGenProvider.class.getName());
            } catch (ClassNotFoundException e) {
                throw new CodeGenException("Failed to load CodeGenProvider class from deployment classloader", e);
            }
            for (CodeGenProvider provider : ServiceLoader.load(codeGenProviderClass, deploymentClassLoader)) {
                Path outputDir = codeGenOutDir(generatedSourcesDir, provider, sourceRegistrar);
                for (Path sourceParentDir : sourceParentDirs) {
                    result.add(
                            new CodeGenData(provider, outputDir, sourceParentDir.resolve(provider.inputDirectory()), buildDir));
                }
            }

            return result;
        });
    }

    private static <T> T callWithClassloader(ClassLoader deploymentClassLoader, CodeGenAction<T> supplier)
            throws CodeGenException {
        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);
            return supplier.fire();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassloader);
        }
    }

    /**
     * generate sources for given code gen
     *
     * @param deploymentClassLoader deployment classloader
     * @param data code gen
     * @param appModel app model
     * @param properties custom code generation properties
     * @return true if sources have been created
     * @throws CodeGenException on failure
     */
    public static boolean trigger(ClassLoader deploymentClassLoader,
            CodeGenData data,
            ApplicationModel appModel,
            Properties properties,
            LaunchMode launchMode) throws CodeGenException {
        return callWithClassloader(deploymentClassLoader, () -> {

            final PropertiesConfigSource pcs = new PropertiesConfigSource(properties, "Build system");

            final SmallRyeConfig config = ConfigUtils.configBuilder(false, launchMode)
                    .withProfile(launchMode.getDefaultProfile())
                    .withSources(pcs)
                    .build();

            CodeGenProvider provider = data.provider;
            return Files.isDirectory(data.sourceDir)
                    && provider.trigger(
                            new CodeGenContext(appModel, data.outPath, data.buildDir, data.sourceDir, data.redirectIO, config));
        });
    }

    private static Path codeGenOutDir(Path generatedSourcesDir,
            CodeGenProvider provider,
            Consumer<Path> sourceRegistrar) throws CodeGenException {
        Path outputDir = generatedSourcesDir.resolve(provider.providerId());
        try {
            Files.createDirectories(outputDir);
            sourceRegistrar.accept(outputDir);
            return outputDir;
        } catch (IOException e) {
            throw new CodeGenException(
                    "Failed to create output directory for generated sources: " + outputDir.toAbsolutePath(), e);
        }
    }

    @FunctionalInterface
    private interface CodeGenAction<T> {
        T fire() throws CodeGenException;
    }
}
