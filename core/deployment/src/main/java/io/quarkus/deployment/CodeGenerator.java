package io.quarkus.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.Config;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.FilteredClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.codegen.CodeGenData;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeContext.ModuleInfo;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.KeyMap;
import io.smallrye.config.KeyMapBackedConfigSource;
import io.smallrye.config.NameIterator;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;

/**
 * A set of methods to initialize and execute {@link CodeGenProvider}s.
 */
public class CodeGenerator {

    private static final String MP_CONFIG_SPI_CONFIG_SOURCE_PROVIDER = "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSourceProvider";

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
     * @param properties custom code generation properties
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
        if (appModel.getAppArtifact().getContentTree()
                .contains(MP_CONFIG_SPI_CONFIG_SOURCE_PROVIDER)) {
            final List<ClassPathElement> allElements = ((QuarkusClassLoader) deploymentClassLoader).getAllElements(false);
            // we don't want to load config sources from the current module because they haven't been compiled yet
            final QuarkusClassLoader.Builder configClBuilder = QuarkusClassLoader
                    .builder("CodeGenerator Config ClassLoader", QuarkusClassLoader.getSystemClassLoader(), false);
            final Collection<Path> appRoots = appModel.getAppArtifact().getContentTree().getRoots();
            for (ClassPathElement e : allElements) {
                if (appRoots.contains(e.getRoot())) {
                    configClBuilder.addElement(new FilteredClassPathElement(e, List.of(MP_CONFIG_SPI_CONFIG_SOURCE_PROVIDER)));
                } else {
                    configClBuilder.addElement(e);
                }
            }
            deploymentClassLoader = configClBuilder.build();
        }
        final SmallRyeConfigBuilder builder = ConfigUtils.configBuilder(false, launchMode)
                .forClassLoader(deploymentClassLoader);
        final PropertiesConfigSource pcs = new PropertiesConfigSource(buildSystemProps, "Build system");
        final SysPropConfigSource spcs = new SysPropConfigSource();

        final Map<String, String> platformProperties = appModel.getPlatformProperties();
        if (platformProperties.isEmpty()) {
            builder.withSources(pcs, spcs);
        } else {
            final KeyMap<String> props = new KeyMap<>(platformProperties.size());
            for (Map.Entry<String, String> prop : platformProperties.entrySet()) {
                props.findOrAdd(new NameIterator(prop.getKey())).putRootValue(prop.getValue());
            }
            final KeyMapBackedConfigSource platformConfigSource = new KeyMapBackedConfigSource("Quarkus platform",
                    // Our default value configuration source is using an ordinal of Integer.MIN_VALUE
                    // (see io.quarkus.deployment.configuration.DefaultValuesConfigurationSource)
                    Integer.MIN_VALUE + 1000, props);
            builder.withSources(platformConfigSource, pcs, spcs);
        }
        return builder.build();
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
