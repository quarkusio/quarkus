package io.quarkus.deployment;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.BuildChain;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveBuildItem;
import io.quarkus.deployment.builditem.AppModelProviderBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.QuarkusBuildCloseablesBuildItem;
import io.quarkus.deployment.builditem.RawCommandLineArgumentsBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.JavaVersionUtil;

public class QuarkusAugmentor {

    private static final Logger log = Logger.getLogger(QuarkusAugmentor.class);

    private final ClassLoader classLoader;
    private final ClassLoader deploymentClassLoader;
    private final PathsCollection root;
    private final Set<Class<? extends BuildItem>> finalResults;
    private final List<Consumer<BuildChainBuilder>> buildChainCustomizers;
    private final LaunchMode launchMode;
    private final DevModeType devModeType;
    private final List<PathsCollection> additionalApplicationArchives;
    private final Collection<Path> excludedFromIndexing;
    private final LiveReloadBuildItem liveReloadBuildItem;
    private final Properties buildSystemProperties;
    private final Path targetDir;
    private final AppModel effectiveModel;
    private final String baseName;
    private final Consumer<ConfigBuilder> configCustomizer;
    private final boolean rebuild;
    private final boolean auxiliaryApplication;
    private final Optional<DevModeType> auxiliaryDevModeType;
    private final boolean test;

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
        this.rebuild = builder.rebuild;
        this.devModeType = builder.devModeType;
        this.auxiliaryApplication = builder.auxiliaryApplication;
        this.auxiliaryDevModeType = Optional.ofNullable(builder.auxiliaryDevModeType);
        this.test = builder.test;
    }

    public BuildResult run() throws Exception {
        if (!JavaVersionUtil.isJava11OrHigher()) {
            throw new IllegalStateException("Quarkus applications require Java 11 or higher to build");
        }
        long time = System.currentTimeMillis();
        log.debug("Beginning Quarkus augmentation");
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        QuarkusBuildCloseablesBuildItem buildCloseables = new QuarkusBuildCloseablesBuildItem();
        try {
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);

            final BuildChainBuilder chainBuilder = BuildChain.builder();
            chainBuilder.setClassLoader(deploymentClassLoader);

            //provideCapabilities(chainBuilder);

            //TODO: we load everything from the deployment class loader
            //this allows the deployment config (application.properties) to be loaded, but in theory could result
            //in additional stuff from the deployment leaking in, this is unlikely but has a bit of a smell.
            ExtensionLoader.loadStepsFrom(deploymentClassLoader,
                    buildSystemProperties == null ? new Properties() : buildSystemProperties,
                    effectiveModel, launchMode, devModeType, configCustomizer)
                    .accept(chainBuilder);

            Thread.currentThread().setContextClassLoader(classLoader);
            chainBuilder.loadProviders(classLoader);

            chainBuilder
                    .addInitial(QuarkusBuildCloseablesBuildItem.class)
                    .addInitial(ArchiveRootBuildItem.class)
                    .addInitial(ShutdownContextBuildItem.class)
                    .addInitial(RawCommandLineArgumentsBuildItem.class)
                    .addInitial(LaunchModeBuildItem.class)
                    .addInitial(LiveReloadBuildItem.class)
                    .addInitial(AdditionalApplicationArchiveBuildItem.class)
                    .addInitial(BuildSystemTargetBuildItem.class)
                    .addInitial(AppModelProviderBuildItem.class);
            for (Class<? extends BuildItem> i : finalResults) {
                chainBuilder.addFinal(i);
            }
            for (Consumer<BuildChainBuilder> i : buildChainCustomizers) {
                i.accept(chainBuilder);
            }

            final ArchiveRootBuildItem.Builder rootBuilder = ArchiveRootBuildItem.builder();
            if (root != null) {
                rootBuilder.addArchiveRoots(root);
            }
            rootBuilder.setExcludedFromIndexing(excludedFromIndexing);

            BuildChain chain = chainBuilder.build();
            BuildExecutionBuilder execBuilder = chain.createExecutionBuilder("main")
                    .produce(buildCloseables)
                    .produce(liveReloadBuildItem)
                    .produce(rootBuilder.build(buildCloseables))
                    .produce(new ShutdownContextBuildItem())
                    .produce(new RawCommandLineArgumentsBuildItem())
                    .produce(new LaunchModeBuildItem(launchMode,
                            devModeType == null ? Optional.empty() : Optional.of(devModeType), auxiliaryApplication,
                            auxiliaryDevModeType, test))
                    .produce(new BuildSystemTargetBuildItem(targetDir, baseName, rebuild,
                            buildSystemProperties == null ? new Properties() : buildSystemProperties))
                    .produce(new AppModelProviderBuildItem(effectiveModel));
            for (PathsCollection i : additionalApplicationArchives) {
                execBuilder.produce(new AdditionalApplicationArchiveBuildItem(i));
            }
            BuildResult buildResult = execBuilder.execute();
            String message = "Quarkus augmentation completed in " + (System.currentTimeMillis() - time) + "ms";
            if (launchMode == LaunchMode.NORMAL) {
                log.info(message);
            } else {
                //test and dev mode already report the total startup time, no need to add noise to the logs
                log.debug(message);
            }
            return buildResult;
        } finally {
            try {
                ConfigProviderResolver.instance()
                        .releaseConfig(ConfigProviderResolver.instance().getConfig(deploymentClassLoader));
            } catch (Exception ignore) {

            }
            if (deploymentClassLoader instanceof Closeable) {
                ((Closeable) deploymentClassLoader).close();
            }
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            buildCloseables.close();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        public DevModeType auxiliaryDevModeType;
        boolean rebuild;
        List<PathsCollection> additionalApplicationArchives = new ArrayList<>();
        Collection<Path> excludedFromIndexing = Collections.emptySet();
        ClassLoader classLoader;
        PathsCollection root;
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
        DevModeType devModeType;
        boolean test;
        boolean auxiliaryApplication;

        public Builder addBuildChainCustomizer(Consumer<BuildChainBuilder> customizer) {
            this.buildChainCustomizers.add(customizer);
            return this;
        }

        public List<PathsCollection> getAdditionalApplicationArchives() {
            return additionalApplicationArchives;
        }

        public Builder addAdditionalApplicationArchive(PathsCollection archive) {
            this.additionalApplicationArchives.add(archive);
            return this;
        }

        public Builder excludeFromIndexing(Collection<Path> excludedFromIndexing) {
            this.excludedFromIndexing = excludedFromIndexing;
            return this;
        }

        public Builder setAuxiliaryApplication(boolean auxiliaryApplication) {
            this.auxiliaryApplication = auxiliaryApplication;
            return this;
        }

        public Builder setAuxiliaryDevModeType(DevModeType auxiliaryDevModeType) {
            this.auxiliaryDevModeType = auxiliaryDevModeType;
            return this;
        }

        public LaunchMode getLaunchMode() {
            return launchMode;
        }

        public Builder setLaunchMode(LaunchMode launchMode) {
            this.launchMode = launchMode;
            return this;
        }

        public DevModeType getDevModeType() {
            return devModeType;
        }

        public Builder setDevModeType(DevModeType devModeType) {
            this.devModeType = devModeType;
            return this;
        }

        public boolean isTest() {
            return test;
        }

        public Builder setTest(boolean test) {
            this.test = test;
            return this;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public Builder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public PathsCollection getRoot() {
            return root;
        }

        public <T extends BuildItem> Builder addFinal(Class<T> clazz) {
            finalResults.add(clazz);
            return this;
        }

        public Builder setRoot(PathsCollection root) {
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

        public Builder setRebuild(boolean rebuild) {
            this.rebuild = rebuild;
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
