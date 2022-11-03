package io.quarkus.deployment;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CracDefaultValueBuildItem;
import io.quarkus.deployment.builditem.CracEnabledBuildItem;
import io.quarkus.deployment.builditem.PreloadClassesEnabledBuildItem;
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
            if (config.enable.get().booleanValue() == false) {
                return;
            }

        } else if (defaultVal == null || !defaultVal.isPresent() || !defaultVal.get().isDefaultValue()) {
            return;
        }
        cracEnabled.produce(CracEnabledBuildItem.INSTANCE);
        if (config.preloadClasses)
            preload.produce(new PreloadClassesEnabledBuildItem());
        crac.register(config.fullWarmup);
    }

}
