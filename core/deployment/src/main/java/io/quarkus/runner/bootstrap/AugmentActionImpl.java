package io.quarkus.runner.bootstrap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.ClassChangeInformation;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.ClassLoaderEventListener;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildChain;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.ExtensionLoader;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceHandledBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.RawCommandLineArgumentsBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;

/**
 * The augmentation task that produces the application.
 */
public class AugmentActionImpl implements AugmentAction {

    private static final Logger log = Logger.getLogger(AugmentActionImpl.class);

    private static final Class[] NON_NORMAL_MODE_OUTPUTS = { GeneratedClassBuildItem.class,
            GeneratedResourceBuildItem.class, ApplicationClassNameBuildItem.class,
            MainClassBuildItem.class, GeneratedFileSystemResourceHandledBuildItem.class,
            TransformedClassesBuildItem.class };

    private final QuarkusBootstrap quarkusBootstrap;
    private final CuratedApplication curatedApplication;
    private final LaunchMode launchMode;
    private final DevModeType devModeType;
    private final List<Consumer<BuildChainBuilder>> chainCustomizers;
    private final List<ClassLoaderEventListener> classLoadListeners;

    /**
     * A map that is shared between all re-runs of the same augment instance. This is
     * only really relevant in dev mode, however it is present in all modes for consistency.
     *
     */
    private final Map<Class<?>, Object> reloadContext = new ConcurrentHashMap<>();

    public AugmentActionImpl(CuratedApplication curatedApplication) {
        this(curatedApplication, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Leaving this here for backwards compatibility, even though this is only internal.
     * 
     * @Deprecated use one of the other constructors
     */
    @Deprecated
    public AugmentActionImpl(CuratedApplication curatedApplication, List<Consumer<BuildChainBuilder>> chainCustomizers) {
        this(curatedApplication, chainCustomizers, Collections.emptyList());
    }

    public AugmentActionImpl(CuratedApplication curatedApplication, List<Consumer<BuildChainBuilder>> chainCustomizers,
            List<ClassLoaderEventListener> classLoadListeners) {
        this.quarkusBootstrap = curatedApplication.getQuarkusBootstrap();
        this.curatedApplication = curatedApplication;
        this.chainCustomizers = chainCustomizers;
        this.classLoadListeners = classLoadListeners;
        LaunchMode launchMode;
        DevModeType devModeType;
        switch (quarkusBootstrap.getMode()) {
            case DEV:
                launchMode = LaunchMode.DEVELOPMENT;
                devModeType = DevModeType.LOCAL;
                break;
            case PROD:
                launchMode = LaunchMode.NORMAL;
                devModeType = null;
                break;
            case TEST:
                launchMode = LaunchMode.TEST;
                devModeType = null;
                break;
            case REMOTE_DEV_CLIENT:
                //this seems a bit counter intuitive, but the remote dev client just keeps a production
                //app up to date and ships it to the remote side, this allows the remote side to be fully up
                //to date even if the process is restarted
                launchMode = LaunchMode.NORMAL;
                devModeType = DevModeType.REMOTE_LOCAL_SIDE;
                break;
            case REMOTE_DEV_SERVER:
                launchMode = LaunchMode.DEVELOPMENT;
                devModeType = DevModeType.REMOTE_SERVER_SIDE;
                break;
            default:
                throw new RuntimeException("Unknown launch mode " + quarkusBootstrap.getMode());
        }
        this.launchMode = launchMode;
        this.devModeType = devModeType;
    }

    @Override
    public AugmentResult createProductionApplication() {
        if (launchMode != LaunchMode.NORMAL) {
            throw new IllegalStateException("Can only create a production application when using NORMAL launch mode");
        }
        ClassLoader classLoader = curatedApplication.createDeploymentClassLoader();
        BuildResult result = runAugment(true, Collections.emptySet(), null, classLoader, ArtifactResultBuildItem.class);

        String debugSourcesDir = BootstrapDebug.DEBUG_SOURCES_DIR;
        if (debugSourcesDir != null) {
            for (GeneratedClassBuildItem i : result.consumeMulti(GeneratedClassBuildItem.class)) {
                try {
                    if (i.getSource() != null) {
                        File debugPath = new File(debugSourcesDir);
                        if (!debugPath.exists()) {
                            debugPath.mkdir();
                        }
                        File sourceFile = new File(debugPath, i.getName() + ".zig");
                        sourceFile.getParentFile().mkdirs();
                        Files.write(sourceFile.toPath(), i.getSource().getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE);
                        log.infof("Wrote source: %s", sourceFile.getAbsolutePath());
                    } else {
                        log.infof("Source not available: %s", i.getName());
                    }
                } catch (Exception t) {
                    log.errorf(t, "Failed to write debug source file: %s", i.getName());
                }
            }
        }

        JarBuildItem jarBuildItem = result.consumeOptional(JarBuildItem.class);
        NativeImageBuildItem nativeImageBuildItem = result.consumeOptional(NativeImageBuildItem.class);
        return new AugmentResult(result.consumeMulti(ArtifactResultBuildItem.class).stream()
                .map(a -> new ArtifactResult(a.getPath(), a.getType(), a.getMetadata()))
                .collect(Collectors.toList()),
                jarBuildItem != null ? jarBuildItem.toJarResult() : null,
                nativeImageBuildItem != null ? nativeImageBuildItem.getPath() : null);
    }

    @Override
    public StartupActionImpl createInitialRuntimeApplication() {
        if (launchMode == LaunchMode.NORMAL) {
            throw new IllegalStateException("Cannot launch a runtime application with NORMAL launch mode");
        }
        ClassLoader classLoader = curatedApplication.createDeploymentClassLoader();
        @SuppressWarnings("unchecked")
        BuildResult result = runAugment(true, Collections.emptySet(), null, classLoader, NON_NORMAL_MODE_OUTPUTS);
        return new StartupActionImpl(curatedApplication, result);
    }

    @Override
    public StartupActionImpl reloadExistingApplication(boolean hasStartedSuccessfully, Set<String> changedResources,
            ClassChangeInformation classChangeInformation) {
        if (launchMode != LaunchMode.DEVELOPMENT) {
            throw new IllegalStateException("Only application with launch mode DEVELOPMENT can restart");
        }
        ClassLoader classLoader = curatedApplication.createDeploymentClassLoader();

        @SuppressWarnings("unchecked")
        BuildResult result = runAugment(!hasStartedSuccessfully, changedResources, classChangeInformation, classLoader,
                NON_NORMAL_MODE_OUTPUTS);

        return new StartupActionImpl(curatedApplication, result);
    }

    /**
     * Runs a custom augmentation action, such as generating config.
     *
     * @param chainBuild A consumer that customises the build to select the output targets
     * @param executionBuild A consumer that can see the initial build execution
     * @return The build result
     */
    public BuildResult runCustomAction(Consumer<BuildChainBuilder> chainBuild, Consumer<BuildExecutionBuilder> executionBuild) {
        ProfileManager.setLaunchMode(launchMode);
        QuarkusClassLoader classLoader = curatedApplication.getAugmentClassLoader();

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);

            final BuildChainBuilder chainBuilder = BuildChain.builder();
            chainBuilder.setClassLoader(classLoader);

            ExtensionLoader.loadStepsFrom(classLoader, new Properties(),
                    curatedApplication.getAppModel().getPlatformProperties(), launchMode, devModeType, null)
                    .accept(chainBuilder);
            chainBuilder.loadProviders(classLoader);

            for (Consumer<BuildChainBuilder> c : chainCustomizers) {
                c.accept(chainBuilder);
            }
            chainBuilder
                    .addInitial(ShutdownContextBuildItem.class)
                    .addInitial(LaunchModeBuildItem.class)
                    .addInitial(LiveReloadBuildItem.class)
                    .addInitial(RawCommandLineArgumentsBuildItem.class)
                    .addFinal(ConfigDescriptionBuildItem.class);
            chainBuild.accept(chainBuilder);

            BuildChain chain = chainBuilder
                    .build();
            BuildExecutionBuilder execBuilder = chain.createExecutionBuilder("main")
                    .produce(new LaunchModeBuildItem(launchMode,
                            devModeType == null ? Optional.empty() : Optional.of(devModeType)))
                    .produce(new ShutdownContextBuildItem())
                    .produce(new RawCommandLineArgumentsBuildItem())
                    .produce(new LiveReloadBuildItem());
            executionBuild.accept(execBuilder);
            return execBuilder
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run task", e);
        } finally {
            try {
                ConfigProviderResolver.instance().releaseConfig(ConfigProviderResolver.instance().getConfig(classLoader));
            } catch (Exception ignore) {

            }
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private BuildResult runAugment(boolean firstRun, Set<String> changedResources,
            ClassChangeInformation classChangeInformation, ClassLoader deploymentClassLoader,
            Class<? extends BuildItem>... finalOutputs) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(curatedApplication.getAugmentClassLoader());
            ProfileManager.setLaunchMode(launchMode);

            QuarkusClassLoader classLoader = curatedApplication.getAugmentClassLoader();

            QuarkusAugmentor.Builder builder = QuarkusAugmentor.builder()
                    .setRoot(quarkusBootstrap.getApplicationRoot())
                    .setClassLoader(classLoader)
                    .addFinal(ApplicationClassNameBuildItem.class)
                    .setTargetDir(quarkusBootstrap.getTargetDirectory())
                    .setDeploymentClassLoader(deploymentClassLoader)
                    .setBuildSystemProperties(curatedApplication.getAppModel().getBuildSystemProperties())
                    .setEffectiveModel(curatedApplication.getAppModel());
            if (quarkusBootstrap.getBaseName() != null) {
                builder.setBaseName(quarkusBootstrap.getBaseName());
            }

            builder.setLaunchMode(launchMode);
            builder.setRebuild(quarkusBootstrap.isRebuild());
            if (firstRun) {
                builder.setLiveReloadState(
                        new LiveReloadBuildItem(false, Collections.emptySet(), reloadContext, classChangeInformation));
            } else {
                builder.setLiveReloadState(
                        new LiveReloadBuildItem(true, changedResources, reloadContext, classChangeInformation));
            }
            for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
                //this gets added to the class path either way
                //but we only need to add it to the additional app archives
                //if it is forced as an app archive
                if (i.isForceApplicationArchive()) {
                    builder.addAdditionalApplicationArchive(i.getArchivePath());
                }
            }
            builder.excludeFromIndexing(quarkusBootstrap.getExcludeFromClassPath());
            for (Consumer<BuildChainBuilder> i : chainCustomizers) {
                builder.addBuildChainCustomizer(i);
            }
            for (Class<? extends BuildItem> i : finalOutputs) {
                builder.addFinal(i);
            }

            try {
                return builder.build().run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    /**
     * A task that can be used in isolated environments to do a build
     */
    @SuppressWarnings("unused")
    public static class BuildTask implements BiConsumer<CuratedApplication, Map<String, Object>> {

        @Override
        public void accept(CuratedApplication application, Map<String, Object> stringObjectMap) {
            AugmentAction action = new AugmentActionImpl(application);
            action.createProductionApplication();
        }
    }
}
