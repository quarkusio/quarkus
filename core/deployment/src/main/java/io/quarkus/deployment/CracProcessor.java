package io.quarkus.deployment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CracDefaultValueBuildItem;
import io.quarkus.deployment.builditem.CracEnabledBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.PreloadClassBuildItem;
import io.quarkus.deployment.builditem.PreloadClassesEnabledBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.runtime.CracRecorder;

public class CracProcessor {

    private static Logger logger = Logger.getLogger(CracProcessor.class);

    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void processCrac(BuildProducer<PreloadClassesEnabledBuildItem> preload,
            BuildProducer<CracEnabledBuildItem> cracEnabled,
            CracRecorder crac,
            CracConfig config,
            Optional<CracDefaultValueBuildItem> defaultVal) {
        if (config.enable.isPresent()) {
            if (!config.enable.get().booleanValue()) {
                return;
            }

        } else if (defaultVal == null || !defaultVal.isPresent() || !defaultVal.get().isDefaultValue()) {
            return;
        }
        cracEnabled.produce(CracEnabledBuildItem.INSTANCE);
        if (config.preloadClasses)
            preload.produce(new PreloadClassesEnabledBuildItem(config.initializeClasses));
        crac.register(config.fullWarmup);
    }

    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    public void generateClassListFromApplication(
            CracConfig cc,
            BuildProducer<PreloadClassBuildItem> producer,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<GeneratedClassBuildItem> generatedClasses) {
        if (cc.generateApplicationClassList) {
            for (Set<TransformedClassesBuildItem.TransformedClass> transformedSet : transformedClasses
                    .getTransformedClassesByJar().values()) {
                for (TransformedClassesBuildItem.TransformedClass transformed : transformedSet) {
                    String className = transformed.getClassName();
                    producer.produce(new PreloadClassBuildItem(className));
                }
            }

            for (GeneratedClassBuildItem i : generatedClasses) {
                if (i.isApplicationClass()) {
                    String cn = i.getName().replace("/", ".");
                    producer.produce(new PreloadClassBuildItem(cn));
                }
            }

            for (ClassInfo clz : applicationArchivesBuildItem.getRootArchive().getIndex().getKnownClasses()) {
                producer.produce(new PreloadClassBuildItem(clz.toString()));
            }
        }
    }

}
