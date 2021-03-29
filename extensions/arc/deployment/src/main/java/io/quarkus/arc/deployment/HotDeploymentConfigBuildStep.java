package io.quarkus.arc.deployment;

import java.util.List;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodParameterInfo;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.dev.DisableInstrumentationForIndexPredicateBuildItem;
import io.quarkus.runtime.StartupEvent;

public class HotDeploymentConfigBuildStep {

    private static final DotName STARTUP_EVENT_NAME = DotName.createSimple(StartupEvent.class.getName());

    @BuildStep
    HotDeploymentWatchedFileBuildItem configFile() {
        return new HotDeploymentWatchedFileBuildItem("META-INF/beans.xml");
    }

    @BuildStep
    DisableInstrumentationForIndexPredicateBuildItem startup() {
        return new DisableInstrumentationForIndexPredicateBuildItem(new Predicate<Index>() {
            @Override
            public boolean test(Index index) {
                if (!index.getAnnotations(StartupBuildSteps.STARTUP_NAME).isEmpty()) {
                    return true;
                }
                List<AnnotationInstance> observesInstances = index.getAnnotations(DotNames.OBSERVES);
                if (!observesInstances.isEmpty()) {
                    for (AnnotationInstance observesInstance : observesInstances) {
                        if (observesInstance.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                            MethodParameterInfo methodParameterInfo = observesInstance.target().asMethodParameter();
                            short paramPos = methodParameterInfo.position();
                            if (STARTUP_EVENT_NAME.equals(methodParameterInfo.method().parameters().get(paramPos).name())) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });
    }

}
