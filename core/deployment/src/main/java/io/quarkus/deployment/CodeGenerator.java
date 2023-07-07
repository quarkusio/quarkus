package io.quarkus.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.codegen.CodeGenData;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeContext.ModuleInfo;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.ClassPathUtils;

/**
 * A set of methods to initialize and execute {@link CodeGenProvider}s.
 */
public class CodeGenerator {

    private static final Logger log = Logger.getLogger(CodeGenerator.class);

    private static final List<String> CONFIG_SERVICES = List.of(
            "META-INF/services/org.eclipse.microprofile.config.spi.Converter",
            "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource",
            "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSourceProvider",
            "META-INF/services/io.smallrye.config.ConfigSourceInterceptor",
            "META-INF/services/io.smallrye.config.ConfigSourceInterceptorFactory",
            "META-INF/services/io.smallrye.config.ConfigSourceFactory",
            "META-INF/services/io.smallrye.config.SecretKeysHandler",
            "META-INF/services/io.smallrye.config.SecretKeysHandlerFactory",
            "META-INF/services/io.smallrye.config.ConfigValidator");

    // used by Gradle and Maven
    public static void initAndRun(QuarkusClassLoader classLoader,
            PathCollection sourceParentDirs, Path generatedSourcesDir, Path buildDir,
            Consumer<Path> sourceRegistrar, ApplicationModel appModel, Properties properties,
            String launchMode, boolean test) throws CodeGenException {
        final List<CodeGenData> generators = init(classLoader, sourceParentDirs, generatedSourcesDir, buildDir,
                sourceRegistrar);
        if (generators.isEmpty()) {
            return;
        }
        final LaunchMode mode = LaunchMode.valueOf(launchMode);
        final Config config = getConfig(appModel, mode, properties, classLoader);
        for (CodeGenData generator : generators) {
            generator.setRedirectIO(true);
            trigger(classLoader, generator, appModel, config, test);
        }
    }

    private static List<CodeGenData> init(ClassLoader deploymentClassLoader,
            PathCollection sourceParentDirs,
            Path generatedSourcesDir,
            Path buildDir,
            Consumer<Path> sourceRegistrar) throws CodeGenException {
        return callWithClassloader(deploymentClassLoader, () -> {
            final List<CodeGenProvider> codeGenProviders = loadCodeGenProviders(deploymentClassLoader);
            if (codeGenProviders.isEmpty()) {
                return List.of();
            }
            final List<CodeGenData> result = new ArrayList<>(codeGenProviders.size());
            for (CodeGenProvider provider : codeGenProviders) {
                Path outputDir = codeGenOutDir(generatedSourcesDir, provider, sourceRegistrar);
                for (Path sourceParentDir : sourceParentDirs) {
                    result.add(
                            new CodeGenData(provider, outputDir, sourceParentDir.resolve(provider.inputDirectory()), buildDir));
                }
            }
            return result;
        });
    }

    public static List<CodeGenData> init(ClassLoader deploymentClassLoader, Collection<ModuleInfo> modules)
            throws CodeGenException {
        return callWithClassloader(deploymentClassLoader, () -> {
            List<CodeGenProvider> codeGenProviders = null;
            List<CodeGenData> codeGens = List.of();
            for (DevModeContext.ModuleInfo module : modules) {
                if (!module.getSourceParents().isEmpty() && module.getPreBuildOutputDir() != null) { // it's null for remote dev

                    if (codeGenProviders == null) {
                        codeGenProviders = loadCodeGenProviders(deploymentClassLoader);
                        if (codeGenProviders.isEmpty()) {
                            return List.of();
                        }
                    }

                    for (CodeGenProvider provider : codeGenProviders) {
                        Path outputDir = codeGenOutDir(Path.of(module.getPreBuildOutputDir()), provider,
                                sourcePath -> module.addSourcePathFirst(sourcePath.toAbsolutePath().toString()));
                        for (Path sourceParentDir : module.getSourceParents()) {
                            if (codeGens.isEmpty()) {
                                codeGens = new ArrayList<>();
                            }
                            codeGens.add(
                                    new CodeGenData(provider, outputDir, sourceParentDir.resolve(provider.inputDirectory()),
                                            Path.of(module.getTargetDir())));
                        }

                    }
                }
            }
            return codeGens;
        });
    }

    @SuppressWarnings("unchecked")
    private static List<CodeGenProvider> loadCodeGenProviders(ClassLoader deploymentClassLoader)
            throws CodeGenException {
        Class<? extends CodeGenProvider> codeGenProviderClass;
        try {
            //noinspection unchecked
            codeGenProviderClass = (Class<? extends CodeGenProvider>) deploymentClassLoader
                    .loadClass(CodeGenProvider.class.getName());
        } catch (ClassNotFoundException e) {
            throw new CodeGenException("Failed to load CodeGenProvider class from deployment classloader", e);
        }
        final Iterator<? extends CodeGenProvider> i = ServiceLoader.load(codeGenProviderClass, deploymentClassLoader)
                .iterator();
        if (!i.hasNext()) {
            return List.of();
        }
        final List<CodeGenProvider> codeGenProviders = new ArrayList<>();
        while (i.hasNext()) {
            codeGenProviders.add(i.next());
        }
        return codeGenProviders;
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
     * @param config config instance
     * @param test whether the sources are generated for production code or tests
     * @return true if sources have been created
     * @throws CodeGenException on failure
     */
    public static boolean trigger(ClassLoader deploymentClassLoader,
            CodeGenData data,
            ApplicationModel appModel,
            Config config,
            boolean test) throws CodeGenException {
        return callWithClassloader(deploymentClassLoader, () -> {
            CodeGenProvider provider = data.provider;
            return provider.shouldRun(data.sourceDir, config)
                    && provider.trigger(
                            new CodeGenContext(appModel, data.outPath, data.buildDir, data.sourceDir, data.redirectIO, config,
                                    test));
        });
    }

    public static Config getConfig(ApplicationModel appModel, LaunchMode launchMode, Properties buildSystemProps,
            QuarkusClassLoader deploymentClassLoader) throws CodeGenException {
        // Config instance that is returned by this method should be as close to the one built in the ExtensionLoader as possible
        final Map<String, List<String>> appModuleConfigServices = getConfigServices(appModel.getAppArtifact());
        if (!appModuleConfigServices.isEmpty()) {
            final Map<String, List<String>> allConfigServices = new HashMap<>(appModuleConfigServices.size());
            final Map<String, byte[]> allowedConfigServices = new HashMap<>(appModuleConfigServices.size());
            final Map<String, byte[]> bannedConfigServices = new HashMap<>(appModuleConfigServices.size());
            for (Map.Entry<String, List<String>> appModuleServices : appModuleConfigServices.entrySet()) {
                final String service = appModuleServices.getKey();
                try {
                    ClassPathUtils.consumeAsPaths(deploymentClassLoader, service, p -> {
                        try {
                            allConfigServices.computeIfAbsent(service, k -> new ArrayList<>())
                                    .addAll(Files.readAllLines(p));
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to read " + p, e);
                        }
                    });
                } catch (IOException e) {
                    throw new CodeGenException("Failed to read resources from classpath", e);
                }
                final List<String> allServices = allConfigServices.getOrDefault(service, List.of());
                allServices.removeAll(appModuleServices.getValue());
                if (allServices.isEmpty()) {
                    bannedConfigServices.put(service, new byte[0]);
                } else {
                    final StringJoiner joiner = new StringJoiner(System.lineSeparator());
                    allServices.forEach(joiner::add);
                    allowedConfigServices.put(service, joiner.toString().getBytes());
                }
            }

            // we don't want to load config services from the current module because they haven't been compiled yet
            final QuarkusClassLoader.Builder configClBuilder = QuarkusClassLoader.builder("CodeGenerator Config ClassLoader",
                    deploymentClassLoader, false);
            if (!allowedConfigServices.isEmpty()) {
                configClBuilder.addElement(new MemoryClassPathElement(allowedConfigServices, true));
            }
            if (!bannedConfigServices.isEmpty()) {
                configClBuilder.addBannedElement(new MemoryClassPathElement(bannedConfigServices, true));
            }
            deploymentClassLoader = configClBuilder.build();
        }
        try {
            return new BuildTimeConfigurationReader(deploymentClassLoader).initConfiguration(launchMode, buildSystemProps,
                    appModel.getPlatformProperties());
        } catch (Exception e) {
            throw new CodeGenException("Failed to initialize application configuration", e);
        } finally {
            if (!appModuleConfigServices.isEmpty()) {
                deploymentClassLoader.close();
            }
        }
    }

    private static Map<String, List<String>> getConfigServices(ResolvedDependency dep) throws CodeGenException {
        final Map<String, List<String>> configServices = new HashMap<>(CONFIG_SERVICES.size());
        try (OpenPathTree openTree = dep.getContentTree().open()) {
            for (String s : CONFIG_SERVICES) {
                openTree.accept(s, v -> {
                    if (v == null) {
                        return;
                    }
                    try {
                        configServices.put(s, Files.readAllLines(v.getPath()));
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read " + v.getPath(), e);
                    }
                });
            }
        } catch (IOException e) {
            throw new CodeGenException("Failed to read " + dep.getResolvedPaths(), e);
        }
        return configServices;
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
