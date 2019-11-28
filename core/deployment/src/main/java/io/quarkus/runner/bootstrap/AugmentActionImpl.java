package io.quarkus.runner.bootstrap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildChain;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.ExtensionLoader;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;

/**
 * The augmentation task that produces the application.
 */
public class AugmentActionImpl implements AugmentAction {

    private final QuarkusBootstrap quarkusBootstrap;
    private final CuratedApplication curatedApplication;
    private final LaunchMode launchMode;
    private final List<Consumer<BuildChainBuilder>> chainCustomizers;

    /**
     * A map that is shared between all re-runs of the same augment instance. This is
     * only really relevant in dev mode, however it is present in all modes for consistency.
     * 
     */
    private final Map<Class<?>, Object> reloadContext = new ConcurrentHashMap<>();

    public AugmentActionImpl(CuratedApplication curatedApplication) {
        this(curatedApplication, Collections.emptyList());
    }

    public AugmentActionImpl(CuratedApplication curatedApplication, List<Consumer<BuildChainBuilder>> chainCustomizers) {
        this.quarkusBootstrap = curatedApplication.getQuarkusBootstrap();
        this.curatedApplication = curatedApplication;
        this.chainCustomizers = chainCustomizers;
        this.launchMode = quarkusBootstrap.getMode() == QuarkusBootstrap.Mode.PROD ? LaunchMode.NORMAL
                : quarkusBootstrap.getMode() == QuarkusBootstrap.Mode.TEST ? LaunchMode.TEST : LaunchMode.DEVELOPMENT;
    }

    @Override
    public AugmentResult createProductionApplication() {
        try {
            if (launchMode != LaunchMode.NORMAL) {
                throw new IllegalStateException("Can only create a production application when using NORMAL launch mode");
            }
            BuildResult result = runAugment(true, Collections.emptySet(), ArtifactResultBuildItem.class);
            JarBuildItem jarBuildItem = result.consumeOptional(JarBuildItem.class);
            NativeImageBuildItem nativeImageBuildItem = result.consumeOptional(NativeImageBuildItem.class);
            return new AugmentResult(result.consumeMulti(ArtifactResultBuildItem.class).stream()
                    .map(a -> new ArtifactResult(a.getPath(), a.getType(), a.getAdditionalPaths()))
                    .collect(Collectors.toList()),
                    jarBuildItem != null ? jarBuildItem.toJarResult() : null,
                    nativeImageBuildItem != null ? nativeImageBuildItem.getPath() : null);
        } finally {
            curatedApplication.close();
        }
    }

    @Override
    public StartupActionImpl createInitialRuntimeApplication() {
        if (launchMode == LaunchMode.NORMAL) {
            throw new IllegalStateException("Cannot launch a runtime application with NORMAL launch mode");
        }
        BuildResult result = runAugment(true, Collections.emptySet(), GeneratedClassBuildItem.class,
                GeneratedResourceBuildItem.class, BytecodeTransformerBuildItem.class, ApplicationClassNameBuildItem.class);
        return new StartupActionImpl(curatedApplication, result);
    }

    @Override
    public StartupActionImpl reloadExistingApplication(Set<String> changedResources) {
        if (launchMode != LaunchMode.DEVELOPMENT) {
            throw new IllegalStateException("Only application with launch mode DEVELOPMENT can restart");
        }
        BuildResult result = runAugment(false, changedResources, GeneratedClassBuildItem.class,
                GeneratedResourceBuildItem.class, BytecodeTransformerBuildItem.class, ApplicationClassNameBuildItem.class);
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

            ExtensionLoader.loadStepsFrom(classLoader).accept(chainBuilder);
            chainBuilder.loadProviders(classLoader);

            for (Consumer<BuildChainBuilder> c : chainCustomizers) {
                c.accept(chainBuilder);
            }
            chainBuilder
                    .addInitial(ShutdownContextBuildItem.class)
                    .addInitial(LaunchModeBuildItem.class)
                    .addInitial(LiveReloadBuildItem.class)
                    .addFinal(ConfigDescriptionBuildItem.class);
            chainBuild.accept(chainBuilder);

            BuildChain chain = chainBuilder
                    .build();
            BuildExecutionBuilder execBuilder = chain.createExecutionBuilder("main")
                    .produce(new LaunchModeBuildItem(launchMode))
                    .produce(new ShutdownContextBuildItem())
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

    private BuildResult runAugment(boolean firstRun, Set<String> changedResources, Class<? extends BuildItem>... finalOutputs) {
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
                    .setDeploymentClassLoader(curatedApplication.createDeploymentClassLoader())
                    .setBuildSystemProperties(quarkusBootstrap.getBuildSystemProperties())
                    .setEffectiveModel(curatedApplication.getAppModel());
            if (quarkusBootstrap.getBaseName() != null) {
                builder.setBaseName(quarkusBootstrap.getBaseName());
            }

            builder.setLaunchMode(launchMode);
            if (firstRun) {
                builder.setLiveReloadState(new LiveReloadBuildItem(false, Collections.emptySet(), reloadContext));
            } else {
                builder.setLiveReloadState(new LiveReloadBuildItem(true, changedResources, reloadContext));
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
