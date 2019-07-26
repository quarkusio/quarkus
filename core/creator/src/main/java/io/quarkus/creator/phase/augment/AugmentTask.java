package io.quarkus.creator.phase.augment;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.DefineClassVisibleURLClassLoader;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.builder.BuildResult;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.CuratedTask;
import io.quarkus.creator.curator.CurateOutcome;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.ThinJarBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarBuildItem;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * This phase consumes {@link CurateOutcome} and processes
 * user application and its dependency classes for phases that generate a runnable application.
 *
 * @author Alexey Loubyansky
 */
public class AugmentTask implements CuratedTask<AugmentOutcome> {

    private static final Logger log = Logger.getLogger(AugmentTask.class);
    private static final String META_INF = "META-INF";

    private final Path outputDir;
    private final Path appClassesDir;
    private final Path configDir;
    private final Properties buildSystemProperties;
    private final Consumer<ConfigBuilder> configCustomizer;

    public AugmentTask(Builder builder) {
        outputDir = builder.outputDir;
        this.appClassesDir = builder.appClassesDir;
        this.configDir = builder.configDir;
        this.buildSystemProperties = builder.buildSystemProperties;
        this.configCustomizer = builder.configCustomizer;
    }

    @Override
    public AugmentOutcome run(CurateOutcome appState, CuratedApplicationCreator ctx) throws AppCreatorException {
        if (this.outputDir != null) {
            IoUtils.mkdirs(outputDir);
        }
        Path outputDir = this.outputDir == null ? ctx.getWorkDir() : this.outputDir;
        Path appClassesDir = this.appClassesDir == null ? outputDir.resolve("classes") : this.appClassesDir;
        if (!Files.exists(appClassesDir)) {
            final Path appJar = appState.getAppArtifact().getPath();
            //manage project without src directory
            if (appJar == null) {
                try {
                    Files.createDirectory(appClassesDir);
                } catch (IOException e) {
                    throw new AppCreatorException("Failed to create classes directory " + appClassesDir, e);
                }
            } else {
                try {
                    ZipUtils.unzip(appJar, appClassesDir);
                } catch (IOException e) {
                    throw new AppCreatorException("Failed to unzip " + appJar, e);
                }
            }
            final Path metaInf = appClassesDir.resolve(META_INF);
            IoUtils.recursiveDelete(metaInf.resolve("maven"));
            IoUtils.recursiveDelete(metaInf.resolve("INDEX.LIST"));
            IoUtils.recursiveDelete(metaInf.resolve("MANIFEST.MF"));
        }
        Path configDir;
        if (this.configDir == null) {
            //lets default to appClassesDir for now
            configDir = appClassesDir;
        } else {
            configDir = this.configDir;
            //if we use gradle we copy the configDir contents to appClassesDir
            try {
                if (Files.exists(this.configDir) && !Files.isSameFile(this.configDir, appClassesDir)) {
                    Files.walkFileTree(configDir,
                            new CopyDirVisitor(configDir, appClassesDir, StandardCopyOption.REPLACE_EXISTING));
                }
            } catch (IOException e) {
                throw new AppCreatorException("Failed while copying files from " + configDir + " to " + appClassesDir, e);
            }
        }
        //first lets look for some config, as it is not on the current class path
        //and we need to load it to run the build process
        Path config = configDir.resolve("application.properties");
        if (Files.exists(config)) {
            try {
                ConfigBuilder builder = SmallRyeConfigProviderResolver.instance().getBuilder()
                        .addDefaultSources()
                        .addDiscoveredConverters()
                        .addDiscoveredSources()
                        .withSources(new PropertiesConfigSource(config.toUri().toURL()));

                if (configCustomizer != null) {
                    configCustomizer.accept(builder);
                }
                SmallRyeConfigProviderResolver.instance().registerConfig(builder.build(),
                        Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (configCustomizer != null) {
            ConfigBuilder builder = SmallRyeConfigProviderResolver.instance().getBuilder()
                    .addDefaultSources()
                    .addDiscoveredConverters()
                    .addDiscoveredSources();

            configCustomizer.accept(builder);
            SmallRyeConfigProviderResolver.instance().registerConfig(builder.build(),
                    Thread.currentThread().getContextClassLoader());
        }

        final AppModelResolver depResolver = appState.getArtifactResolver();
        List<AppDependency> appDeps;
        try {
            appDeps = appState.getEffectiveModel().getAllDependencies();
        } catch (BootstrapDependencyProcessingException e) {
            throw new AppCreatorException("Failed to resolve application build classpath", e);
        }

        URLClassLoader runnerClassLoader = null;
        try {
            // we need to make sure all the deployment artifacts are on the class path
            final List<URL> cpUrls = new ArrayList<>(appDeps.size() + 1);
            cpUrls.add(appClassesDir.toUri().toURL());

            for (AppDependency appDep : appDeps) {
                final Path resolvedDep = depResolver.resolve(appDep.getArtifact());
                cpUrls.add(resolvedDep.toUri().toURL());
            }

            runnerClassLoader = new DefineClassVisibleURLClassLoader(cpUrls.toArray(new URL[cpUrls.size()]),
                    getClass().getClassLoader());

            ClassLoader old = Thread.currentThread().getContextClassLoader();
            BuildResult result;
            try {
                Thread.currentThread().setContextClassLoader(runnerClassLoader);

                QuarkusAugmentor.Builder builder = QuarkusAugmentor.builder();
                builder.setRoot(appClassesDir);
                builder.setBaseName(ctx.getBaseName());
                builder.setTargetDir(outputDir);
                builder.setResolver(appState.getArtifactResolver());
                builder.setEffectiveModel(appState.getEffectiveModel());
                builder.setClassLoader(runnerClassLoader);
                builder.setConfigCustomizer(configCustomizer);
                builder.setBuildSystemProperties(buildSystemProperties);
                builder.addFinal(BytecodeTransformerBuildItem.class)
                        .addFinal(ApplicationArchivesBuildItem.class)
                        .addFinal(MainClassBuildItem.class)
                        .addFinal(ArtifactResultBuildItem.class);
                result = builder.build().run();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
            return new AugmentOutcome(result.consumeMulti(ArtifactResultBuildItem.class),
                    result.consumeOptional(ThinJarBuildItem.class), result.consumeOptional(UberJarBuildItem.class),
                    result.consumeOptional(NativeImageBuildItem.class));

        } catch (Exception e) {
            throw new AppCreatorException("Failed to augment application classes", e);
        } finally {
            if (runnerClassLoader != null) {
                try {
                    runnerClassLoader.close();
                } catch (IOException e) {
                    log.warn("Failed to close runner classloader", e);
                }
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public Path getAppClassesDir() {
        return appClassesDir;
    }

    public Path getConfigDir() {
        return configDir;
    }

    public Properties getBuildSystemProperties() {
        return buildSystemProperties;
    }

    public static class CopyDirVisitor extends SimpleFileVisitor<Path> {
        private final Path fromPath;
        private final Path toPath;
        private final CopyOption copyOption;

        public CopyDirVisitor(Path fromPath, Path toPath, CopyOption copyOption) {
            this.fromPath = fromPath;
            this.toPath = toPath;
            this.copyOption = copyOption;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = toPath.resolve(fromPath.relativize(dir));
            if (!Files.exists(targetPath)) {
                Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
            return FileVisitResult.CONTINUE;
        }
    }

    public static class Builder {

        private Path outputDir;
        private Path appClassesDir;
        private Path configDir;
        private Properties buildSystemProperties;
        private Consumer<ConfigBuilder> configCustomizer;

        /**
         * Output directory for the outcome of this phase.
         * If not set by the user the work directory of the creator
         * will be used instead.
         *
         * @param outputDir output directory for this phase
         * @return this phase instance
         */
        public Builder setOutputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        /**
         * Directory containing application classes. If none is set by the user,
         * the creation process has to be initiated with an application JAR which
         * will be unpacked into classes directory in the creator's work directory.
         *
         * @param appClassesDir directory for application classes
         * @return this phase instance
         */
        public Builder setAppClassesDir(Path appClassesDir) {
            this.appClassesDir = appClassesDir;
            return this;
        }

        /**
         * Directory containing the configuration files.
         *
         * @param configDir directory the configuration files (application.properties)
         * @return this phase instance
         */
        public Builder setConfigDir(Path configDir) {
            this.configDir = configDir;
            return this;
        }

        /**
         * Set the build system's properties, if any.
         *
         * @param buildSystemProperties the build system properties or {@code null} to unset
         * @return this phase instance
         */
        public Builder setBuildSystemProperties(final Properties buildSystemProperties) {
            this.buildSystemProperties = buildSystemProperties;
            return this;
        }

        public Builder setConfigCustomizer(Consumer<ConfigBuilder> configCustomizer) {
            this.configCustomizer = configCustomizer;
            return this;
        }

        public AugmentTask build() {
            return new AugmentTask(this);
        }
    }
}
