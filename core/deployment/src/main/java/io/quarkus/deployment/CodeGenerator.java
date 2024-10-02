package io.quarkus.deployment;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.codegen.CodeGenData;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.tracker.ConfigTrackingConfig;
import io.quarkus.deployment.configuration.tracker.ConfigTrackingValueTransformer;
import io.quarkus.deployment.configuration.tracker.ConfigTrackingWriter;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeContext.ModuleInfo;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.ClassPathUtils;
import io.smallrye.config.SmallRyeConfig;

/**
 * A set of methods to initialize and execute {@link CodeGenProvider}s.
 */
public class CodeGenerator {

    private static final Logger log = Logger.getLogger(CodeGenerator.class);

    private static final String META_INF_SERVICES = "META-INF/services/";

    private static final List<String> CONFIG_SERVICES = List.of(
            "org.eclipse.microprofile.config.spi.Converter",
            "org.eclipse.microprofile.config.spi.ConfigSource",
            "org.eclipse.microprofile.config.spi.ConfigSourceProvider",
            "io.smallrye.config.ConfigSourceInterceptor",
            "io.smallrye.config.ConfigSourceInterceptorFactory",
            "io.smallrye.config.ConfigSourceFactory",
            "io.smallrye.config.SecretKeysHandler",
            "io.smallrye.config.SecretKeysHandlerFactory",
            "io.smallrye.config.ConfigValidator");

    // used by Gradle and Maven
    public static void initAndRun(QuarkusClassLoader classLoader,
            PathCollection sourceParentDirs, Path generatedSourcesDir, Path buildDir,
            Consumer<Path> sourceRegistrar, ApplicationModel appModel, Properties properties,
            String launchMode, boolean test) throws CodeGenException {
        Map<String, String> props = new HashMap<>();
        properties.entrySet().stream().forEach(e -> props.put((String) e.getKey(), (String) e.getValue()));
        final List<CodeGenData> generators = init(appModel, props, classLoader, sourceParentDirs, generatedSourcesDir, buildDir,
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

    private static List<CodeGenData> init(
            ApplicationModel model,
            Map<String, String> properties,
            ClassLoader deploymentClassLoader,
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
                provider.init(model, properties);
                Path outputDir = codeGenOutDir(generatedSourcesDir, provider, sourceRegistrar);
                for (Path sourceParentDir : sourceParentDirs) {
                    Path in = provider.getInputDirectory();
                    if (in == null) {
                        in = sourceParentDir.resolve(provider.inputDirectory());
                    }
                    result.add(
                            new CodeGenData(provider, outputDir, in, buildDir));
                }
            }
            return result;
        });
    }

    public static List<CodeGenData> init(ApplicationModel model, Map<String, String> properties,
            ClassLoader deploymentClassLoader, Collection<ModuleInfo> modules)
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
                        provider.init(model, properties);
                        Path outputDir = codeGenOutDir(Path.of(module.getPreBuildOutputDir()), provider,
                                sourcePath -> module.addSourcePathFirst(sourcePath.toAbsolutePath().toString()));
                        for (Path sourceParentDir : module.getSourceParents()) {
                            if (codeGens.isEmpty()) {
                                codeGens = new ArrayList<>();
                            }
                            Path in = provider.getInputDirectory();
                            if (in == null) {
                                in = sourceParentDir.resolve(provider.inputDirectory());
                            }
                            codeGens.add(
                                    new CodeGenData(provider, outputDir, in,
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

    /**
     * Initializes an application build time configuration and dumps current values of properties
     * passed in as {@code previouslyRecordedProperties} to a file.
     *
     * @param appModel application model
     * @param launchMode launch mode
     * @param buildSystemProps build system (or project) properties
     * @param deploymentClassLoader build classloader
     * @param previouslyRecordedProperties properties to read from the initialized configuration
     * @param outputFile output file
     */
    public static void dumpCurrentConfigValues(ApplicationModel appModel, String launchMode, Properties buildSystemProps,
            QuarkusClassLoader deploymentClassLoader, Properties previouslyRecordedProperties,
            Path outputFile) {
        final LaunchMode mode = LaunchMode.valueOf(launchMode);
        if (previouslyRecordedProperties.isEmpty()) {
            try {
                readConfig(appModel, mode, buildSystemProps, deploymentClassLoader, configReader -> {
                    var config = configReader.initConfiguration(mode, buildSystemProps, new Properties(),
                            appModel.getPlatformProperties());
                    final Map<String, String> allProps = new HashMap<>();
                    for (String name : config.getPropertyNames()) {
                        allProps.put(name, ConfigTrackingValueTransformer.asString(config.getConfigValue(name)));
                    }
                    ConfigTrackingWriter.write(allProps,
                            config.unwrap(SmallRyeConfig.class).getConfigMapping(ConfigTrackingConfig.class),
                            configReader.readConfiguration(config),
                            outputFile);
                    return null;
                });
            } catch (CodeGenException e) {
                throw new RuntimeException("Failed to load application configuration", e);
            }
            return;
        }
        Config config = null;
        try {
            config = getConfig(appModel, mode, buildSystemProps, deploymentClassLoader);
        } catch (CodeGenException e) {
            throw new RuntimeException("Failed to load application configuration", e);
        }
        var valueTransformer = ConfigTrackingValueTransformer.newInstance(config);
        final Properties currentValues = new Properties(previouslyRecordedProperties.size());
        for (var prevProp : previouslyRecordedProperties.entrySet()) {
            var name = prevProp.getKey().toString();
            var currentValue = config.getConfigValue(name);
            final String current = valueTransformer.transform(name, currentValue);
            var originalValue = prevProp.getValue();
            if (!originalValue.equals(current)) {
                log.info("Option " + name + " has changed since the last build from " + originalValue + " to " + current);
            }
            if (current != null) {
                currentValues.put(name, current);
            }
        }

        final List<String> names = new ArrayList<>(currentValues.stringPropertyNames());
        Collections.sort(names);

        final Path outputDir = outputFile.getParent();
        if (outputDir != null && !Files.exists(outputDir)) {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            for (var name : names) {
                ConfigTrackingWriter.write(writer, name, currentValues.getProperty(name));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Config getConfig(ApplicationModel appModel, LaunchMode launchMode, Properties buildSystemProps,
            QuarkusClassLoader deploymentClassLoader) throws CodeGenException {
        return readConfig(appModel, launchMode, buildSystemProps, deploymentClassLoader,
                configReader -> configReader.initConfiguration(launchMode, buildSystemProps, new Properties(),
                        appModel.getPlatformProperties()));
    }

    public static <T> T readConfig(ApplicationModel appModel, LaunchMode launchMode, Properties buildSystemProps,
            QuarkusClassLoader deploymentClassLoader, Function<BuildTimeConfigurationReader, T> function)
            throws CodeGenException {
        final Map<String, List<String>> unavailableConfigServices = getUnavailableConfigServices(appModel.getAppArtifact(),
                deploymentClassLoader);
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        if (!unavailableConfigServices.isEmpty()) {
            var sb = new StringBuilder();
            sb.append(
                    "The following services are not (yet) available and will be disabled during configuration initialization at the current build phase:");
            for (Map.Entry<String, List<String>> missingService : unavailableConfigServices.entrySet()) {
                sb.append(System.lineSeparator());
                for (String s : missingService.getValue()) {
                    sb.append("- ").append(s);
                }
            }
            log.warn(sb.toString());

            final Map<String, List<String>> allConfigServices = new HashMap<>(unavailableConfigServices.size());
            final Map<String, byte[]> allowedConfigServices = new HashMap<>(unavailableConfigServices.size());
            final Map<String, byte[]> bannedConfigServices = new HashMap<>(unavailableConfigServices.size());
            for (Map.Entry<String, List<String>> appModuleServices : unavailableConfigServices.entrySet()) {
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
                configClBuilder.addNormalPriorityElement(new MemoryClassPathElement(allowedConfigServices, true));
            }
            if (!bannedConfigServices.isEmpty()) {
                configClBuilder.addBannedElement(new MemoryClassPathElement(bannedConfigServices, true));
            }
            deploymentClassLoader = configClBuilder.build();
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);
        }
        try {
            return function.apply(new BuildTimeConfigurationReader(deploymentClassLoader));
        } catch (Exception e) {
            throw new CodeGenException("Failed to initialize application configuration", e);
        } finally {
            if (!unavailableConfigServices.isEmpty()) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
                deploymentClassLoader.close();
            }
        }
    }

    private static Map<String, List<String>> getUnavailableConfigServices(ResolvedDependency dep, ClassLoader classLoader)
            throws CodeGenException {
        try (OpenPathTree openTree = dep.getContentTree().open()) {
            var unavailableServices = new HashMap<String, List<String>>();
            openTree.apply(META_INF_SERVICES, visit -> {
                if (visit == null) {
                    // the application module does not include META-INF/services entry. Return `null` here, to let
                    // MultiRootPathTree.apply() look into all roots.
                    return null;
                }
                var servicesDir = visit.getPath();
                for (String serviceClass : CONFIG_SERVICES) {
                    var serviceFile = servicesDir.resolve(serviceClass);
                    if (!Files.exists(serviceFile)) {
                        continue;
                    }
                    var unavailableList = unavailableServices.computeIfAbsent(META_INF_SERVICES + serviceClass,
                            k -> new ArrayList<>());
                    try {
                        Files.readAllLines(serviceFile).stream()
                                .map(String::trim)
                                // skip comments and empty lines
                                .filter(line -> !line.startsWith("#") && !line.isEmpty())
                                .filter(className -> classLoader.getResource(fromClassNameToResourceName(className)) == null)
                                .forEach(unavailableList::add);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read " + serviceFile, e);
                    }
                }
                // Always return null to let MultiRootPathTree.apply() look into all roots.
                return null;
            });
            return unavailableServices;
        } catch (IOException e) {
            throw new CodeGenException("Failed to read " + dep.getResolvedPaths(), e);
        }
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
