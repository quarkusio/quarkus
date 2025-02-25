package io.quarkus.deployment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.ClassInfo;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.PreloadClassBuildItem;
import io.quarkus.deployment.builditem.PreloadClassesEnabledBuildItem;
import io.quarkus.deployment.builditem.SnapStartDefaultValueBuildItem;
import io.quarkus.deployment.builditem.SnapStartEnabledBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.runtime.SnapStartRecorder;

/**
 * A processor handling the various AWS SnapStart optimizations.
 */
public class SnapStartProcessor {

    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void processSnapStart(BuildProducer<PreloadClassesEnabledBuildItem> preload,
            BuildProducer<SnapStartEnabledBuildItem> snapStartEnabled,
            SnapStartRecorder recorder,
            SnapStartConfig config,
            Optional<SnapStartDefaultValueBuildItem> defaultVal) {
        if (config.enable().isPresent()) {
            if (!config.enable().get().booleanValue()) {
                return;
            }

        } else if (defaultVal == null || !defaultVal.isPresent() || !defaultVal.get().isDefaultValue()) {
            return;
        }
        snapStartEnabled.produce(SnapStartEnabledBuildItem.INSTANCE);
        if (config.preloadClasses())
            preload.produce(new PreloadClassesEnabledBuildItem(config.initializeClasses()));
        recorder.register(config.fullWarmup());
    }

    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    public void generateClassListFromApplication(
            SnapStartConfig config,
            Optional<SnapStartDefaultValueBuildItem> defaultVal,
            BuildProducer<PreloadClassBuildItem> producer,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<GeneratedClassBuildItem> generatedClasses) {
        if (config.enable().isPresent()) {
            if (!config.enable().get()) {
                return;
            }
        } else if (defaultVal == null || !defaultVal.isPresent() || !defaultVal.get().isDefaultValue()) {
            return;
        }

        if (config.generateApplicationClassList()) {
            for (Set<TransformedClassesBuildItem.TransformedClass> transformedSet : transformedClasses
                    .getTransformedClassesByJar().values()) {
                for (TransformedClassesBuildItem.TransformedClass transformed : transformedSet) {
                    String className = transformed.getClassName();
                    if (className != null) {
                        producer.produce(new PreloadClassBuildItem(className));
                    }
                }
            }

            for (GeneratedClassBuildItem i : generatedClasses) {
                if (i.isApplicationClass()) {
                    if (i.getName() != null) {
                        String cn = i.getName().replace("/", ".");
                        producer.produce(new PreloadClassBuildItem(cn));
                    }
                }
            }

            for (ClassInfo clz : applicationArchivesBuildItem.getRootArchive().getIndex().getKnownClasses()) {
                producer.produce(new PreloadClassBuildItem(clz.toString()));
            }
        }
    }

}
