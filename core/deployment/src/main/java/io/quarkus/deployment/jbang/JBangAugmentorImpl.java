package io.quarkus.deployment.jbang;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.BuildStepBuilder;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.runtime.LaunchMode;

public class JBangAugmentorImpl implements BiConsumer<CuratedApplication, Map<String, Object>> {

    private static final Logger log = Logger.getLogger(JBangAugmentorImpl.class);

    @Override
    public void accept(CuratedApplication curatedApplication, Map<String, Object> resultMap) {

        QuarkusClassLoader classLoader = curatedApplication.getAugmentClassLoader();

        QuarkusBootstrap quarkusBootstrap = curatedApplication.getQuarkusBootstrap();
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

        boolean auxiliaryApplication = curatedApplication.getQuarkusBootstrap().isAuxiliaryApplication();
        builder.setAuxiliaryApplication(auxiliaryApplication);
        builder.setAuxiliaryDevModeType(
                curatedApplication.getQuarkusBootstrap().isHostApplicationIsTestOnly() ? DevModeType.TEST_ONLY
                        : (auxiliaryApplication ? DevModeType.LOCAL : null));
        builder.setLaunchMode(LaunchMode.NORMAL);
        builder.setRebuild(quarkusBootstrap.isRebuild());
        builder.setLiveReloadState(
                new LiveReloadBuildItem(false, Collections.emptySet(), new HashMap<>(), null));
        for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
            //this gets added to the class path either way
            //but we only need to add it to the additional app archives
            //if it is forced as an app archive
            if (i.isForceApplicationArchive()) {
                builder.addAdditionalApplicationArchive(i.getArchivePath());
            }
        }
        builder.addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                final BuildStepBuilder stepBuilder = builder.addBuildStep((ctx) -> {
                    ctx.produce(new ProcessInheritIODisabled());
                });
                stepBuilder.produces(ProcessInheritIODisabled.class).build();
            }
        });
        builder.excludeFromIndexing(quarkusBootstrap.getExcludeFromClassPath());
        builder.addFinal(GeneratedClassBuildItem.class);
        builder.addFinal(MainClassBuildItem.class);
        builder.addFinal(GeneratedResourceBuildItem.class);
        builder.addFinal(TransformedClassesBuildItem.class);
        builder.addFinal(DeploymentResultBuildItem.class);
        boolean nativeRequested = "native".equals(System.getProperty("quarkus.package.type"));
        boolean containerBuildRequested = Boolean.getBoolean("quarkus.container-image.build");
        if (nativeRequested) {
            builder.addFinal(NativeImageBuildItem.class);
        }
        if (containerBuildRequested) {
            //TODO: this is a bit ugly
            //we don't nessesarily need these artifacts
            //but if we include them it does mean that you can auto create docker images
            //and deploy to kube etc
            //for an ordinary build with no native and no docker this is a waste
            builder.addFinal(ArtifactResultBuildItem.class);
        }

        try {
            BuildResult buildResult = builder.build().run();
            Map<String, byte[]> result = new HashMap<>();
            for (GeneratedClassBuildItem i : buildResult.consumeMulti(GeneratedClassBuildItem.class)) {
                result.put(i.getName().replace(".", "/") + ".class", i.getClassData());
            }
            for (GeneratedResourceBuildItem i : buildResult.consumeMulti(GeneratedResourceBuildItem.class)) {
                result.put(i.getName(), i.getClassData());
            }
            for (Map.Entry<Path, Set<TransformedClassesBuildItem.TransformedClass>> entry : buildResult
                    .consume(TransformedClassesBuildItem.class).getTransformedClassesByJar().entrySet()) {
                for (TransformedClassesBuildItem.TransformedClass transformed : entry.getValue()) {
                    if (transformed.getData() != null) {
                        result.put(transformed.getFileName(), transformed.getData());
                    } else {
                        log.warn("Unable to remove resource " + transformed.getFileName()
                                + " as this is not supported in JBangf");
                    }
                }
            }
            resultMap.put("files", result);
            List javaargs = new ArrayList<String>();
            javaargs.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
            resultMap.put("java-args", javaargs);
            resultMap.put("main-class", buildResult.consume(MainClassBuildItem.class).getClassName());
            if (nativeRequested) {
                resultMap.put("native-image", buildResult.consume(NativeImageBuildItem.class).getPath());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
