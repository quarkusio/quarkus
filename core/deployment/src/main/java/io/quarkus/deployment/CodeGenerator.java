package io.quarkus.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.codegen.CodeGenData;

public class CodeGenerator {

    // used by Gradle
    @SuppressWarnings("unused")
    public static void initAndRun(ClassLoader classLoader,
            Path sourceParentDir, Path generatedSourcesDir, Path buildDir,
            Consumer<Path> sourceRegistrar,
            AppModel appModel) throws CodeGenException {
        List<CodeGenData> generators = init(classLoader, sourceParentDir, generatedSourcesDir, buildDir, sourceRegistrar);
        for (CodeGenData generator : generators) {
            trigger(classLoader, generator, appModel);
        }

    }

    public static List<CodeGenData> init(ClassLoader deploymentClassLoader,
            Path sourceParentDir,
            Path generatedSourcesDir,
            Path buildDir,
            Consumer<Path> sourceRegistrar) throws CodeGenException {
        return callWithClassloader(deploymentClassLoader, () -> {
            List<CodeGenData> result = new ArrayList<>();
            Class<? extends CodeGenProvider> codeGenProviderClass;
            try {
                codeGenProviderClass = (Class<? extends CodeGenProvider>) deploymentClassLoader
                        .loadClass(CodeGenProvider.class.getName());
            } catch (ClassNotFoundException e) {
                throw new CodeGenException("Failde to load CodeGenProvider class from deployment classloader", e);
            }
            for (CodeGenProvider provider : ServiceLoader.load(codeGenProviderClass)) {
                Path outputDir = codeGenOutDir(generatedSourcesDir, provider, sourceRegistrar);
                result.add(new CodeGenData(provider, outputDir, sourceParentDir.resolve(provider.inputDirectory()), buildDir));
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
     * @return true if sources have been created
     * @throws CodeGenException on failure
     */
    public static boolean trigger(ClassLoader deploymentClassLoader,
            CodeGenData data,
            AppModel appModel) throws CodeGenException {
        return callWithClassloader(deploymentClassLoader, () -> {
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);

            CodeGenProvider provider = data.provider;

            return Files.isDirectory(data.sourceDir)
                    && provider.trigger(new CodeGenContext(appModel, data.outPath, data.buildDir, data.sourceDir));
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
