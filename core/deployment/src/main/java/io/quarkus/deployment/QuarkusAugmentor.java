package io.quarkus.deployment;

import java.io.Closeable;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.builder.BuildChain;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.DeploymentClassLoaderBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.LaunchMode;

public class QuarkusAugmentor {

    private static final Logger log = Logger.getLogger(QuarkusAugmentor.class);

    private final ClassLoader classLoader;
    private final ClassLoader deploymentClassLoader;
    private final Path root;
    private final Set<Class<? extends BuildItem>> finalResults;
    private final List<Consumer<BuildChainBuilder>> buildChainCustomizers;
    private final LaunchMode launchMode;
    private final List<Path> additionalApplicationArchives;
    private final Collection<Path> excludedFromIndexing;
    private final LiveReloadBuildItem liveReloadBuildItem;
    private final Properties buildSystemProperties;
    private final Path targetDir;
    private final AppModel effectiveModel;
    private final String baseName;
    private final Consumer<ConfigBuilder> configCustomizer;

    QuarkusAugmentor(Builder builder) {
        this.classLoader = builder.classLoader;
        this.root = builder.root;
        this.finalResults = new HashSet<>(builder.finalResults);
        this.buildChainCustomizers = new ArrayList<>(builder.buildChainCustomizers);
        this.launchMode = builder.launchMode;
        this.additionalApplicationArchives = new ArrayList<>(builder.additionalApplicationArchives);
        this.excludedFromIndexing = builder.excludedFromIndexing;
        this.liveReloadBuildItem = builder.liveReloadState;
        this.buildSystemProperties = builder.buildSystemProperties;
        this.targetDir = builder.targetDir;
        this.effectiveModel = builder.effectiveModel;
        this.baseName = builder.baseName;
        this.configCustomizer = builder.configCustomizer;
        this.deploymentClassLoader = builder.deploymentClassLoader;
    }

    public BuildResult run() throws Exception {
        long time = System.currentTimeMillis();
        log.debug("Beginning Quarkus augmentation");
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        FileSystem rootFs = null;
        try {
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);

            final BuildChainBuilder chainBuilder = BuildChain.builder();

            //TODO: we load everything from the deployment class loader
            //this allows the deployment config (application.properties) to be loaded, but in theory could result
            //in additional stuff from the deployment leaking in, this is unlikely but has a bit of a smell.
            if (buildSystemProperties != null) {
                ExtensionLoader.loadStepsFrom(deploymentClassLoader, buildSystemProperties, launchMode, configCustomizer)
                        .accept(chainBuilder);
            } else {
                ExtensionLoader.loadStepsFrom(deploymentClassLoader, launchMode, configCustomizer).accept(chainBuilder);
            }
            Thread.currentThread().setContextClassLoader(classLoader);
            chainBuilder.loadProviders(classLoader);

            chainBuilder
                    .addInitial(DeploymentClassLoaderBuildItem.class)
                    .addInitial(ArchiveRootBuildItem.class)
                    .addInitial(ShutdownContextBuildItem.class)
                    .addInitial(LaunchModeBuildItem.class)
                    .addInitial(LiveReloadBuildItem.class)
                    .addInitial(AdditionalApplicationArchiveBuildItem.class)
                    .addInitial(BuildSystemTargetBuildItem.class)
                    .addInitial(CurateOutcomeBuildItem.class);
            for (Class<? extends BuildItem> i : finalResults) {
                chainBuilder.addFinal(i);
            }
            chainBuilder.addFinal(GeneratedClassBuildItem.class)
                    .addFinal(GeneratedResourceBuildItem.class);

            for (Consumer<BuildChainBuilder> i : buildChainCustomizers) {
                i.accept(chainBuilder);
            }

            BuildChain chain = chainBuilder
                    .build();
            if (!Files.isDirectory(root)) {
                rootFs = FileSystems.newFileSystem(root, null);
            }
            BuildExecutionBuilder execBuilder = chain.createExecutionBuilder("main")
                    .produce(liveReloadBuildItem)
                    .produce(new ArchiveRootBuildItem(root, rootFs == null ? root : rootFs.getPath("/"), excludedFromIndexing))
                    .produce(new ShutdownContextBuildItem())
                    .produce(new LaunchModeBuildItem(launchMode))
                    .produce(new BuildSystemTargetBuildItem(targetDir, baseName))
                    .produce(new DeploymentClassLoaderBuildItem(deploymentClassLoader))
                    .produce(new CurateOutcomeBuildItem(effectiveModel));
            for (Path i : additionalApplicationArchives) {
                execBuilder.produce(new AdditionalApplicationArchiveBuildItem(i));
            }
            BuildResult buildResult = execBuilder
                    .execute();
            String message = "Quarkus augmentation completed in " + (System.currentTimeMillis() - time) + "ms";
            if (launchMode == LaunchMode.NORMAL) {
                log.info(message);
            } else {
                //test and dev mode already report the total startup time, no need to add noise to the logs
                log.debug(message);
            }
            return buildResult;
        } finally {
            if (rootFs != null) {
                try {
                    rootFs.close();
                } catch (Exception e) {
                }
            }
            try {
                ConfigProviderResolver.instance()
                        .releaseConfig(ConfigProviderResolver.instance().getConfig(deploymentClassLoader));
            } catch (Exception ignore) {

            }
            if (deploymentClassLoader instanceof Closeable) {
                ((Closeable) deploymentClassLoader).close();
            }
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        List<Path> additionalApplicationArchives = new ArrayList<>();
        Collection<Path> excludedFromIndexing = Collections.emptySet();
        ClassLoader classLoader;
        Path root;
        Path targetDir;
        Set<Class<? extends BuildItem>> finalResults = new HashSet<>();
        private final List<Consumer<BuildChainBuilder>> buildChainCustomizers = new ArrayList<>();
        LaunchMode launchMode = LaunchMode.NORMAL;
        LiveReloadBuildItem liveReloadState = new LiveReloadBuildItem();
        Properties buildSystemProperties;

        AppModel effectiveModel;
        String baseName = "quarkus-application";
        Consumer<ConfigBuilder> configCustomizer;
        ClassLoader deploymentClassLoader;

        public Builder addBuildChainCustomizer(Consumer<BuildChainBuilder> customizer) {
            this.buildChainCustomizers.add(customizer);
            return this;
        }

        public List<Path> getAdditionalApplicationArchives() {
            return additionalApplicationArchives;
        }

        public Builder addAdditionalApplicationArchive(Path archive) {
            this.additionalApplicationArchives.add(archive);
            return this;
        }

        public Builder excludeFromIndexing(Collection<Path> excludedFromIndexing) {
            this.excludedFromIndexing = excludedFromIndexing;
            return this;
        }

        public LaunchMode getLaunchMode() {
            return launchMode;
        }

        public Builder setLaunchMode(LaunchMode launchMode) {
            this.launchMode = launchMode;
            return this;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public Builder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Path getRoot() {
            return root;
        }

        public <T extends BuildItem> Builder addFinal(Class<T> clazz) {
            finalResults.add(clazz);
            return this;
        }

        public Builder setRoot(Path root) {
            this.root = root;
            return this;
        }

        public String getBaseName() {
            return baseName;
        }

        public Builder setBaseName(String baseName) {
            this.baseName = baseName;
            return this;
        }

        public Properties getBuildSystemProperties() {
            return buildSystemProperties;
        }

        public Builder setBuildSystemProperties(final Properties buildSystemProperties) {
            this.buildSystemProperties = buildSystemProperties;
            return this;
        }

        public QuarkusAugmentor build() {
            return new QuarkusAugmentor(this);
        }

        public LiveReloadBuildItem getLiveReloadState() {
            return liveReloadState;
        }

        public Builder setLiveReloadState(LiveReloadBuildItem liveReloadState) {
            this.liveReloadState = liveReloadState;
            return this;
        }

        public Builder setTargetDir(Path outputDir) {
            targetDir = outputDir;
            return this;
        }

        public Builder setEffectiveModel(AppModel effectiveModel) {
            this.effectiveModel = effectiveModel;
            return this;
        }

        public ClassLoader getDeploymentClassLoader() {
            return deploymentClassLoader;
        }

        public Builder setDeploymentClassLoader(ClassLoader deploymentClassLoader) {
            this.deploymentClassLoader = deploymentClassLoader;
            return this;
        }

        public Builder setConfigCustomizer(Consumer<ConfigBuilder> configCustomizer) {
            this.configCustomizer = configCustomizer;
            return this;
        }
    }
}
