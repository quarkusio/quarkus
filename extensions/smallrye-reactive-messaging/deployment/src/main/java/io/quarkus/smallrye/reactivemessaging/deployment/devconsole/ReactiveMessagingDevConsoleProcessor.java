package io.quarkus.smallrye.reactivemessaging.deployment.devconsole;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames;
import io.quarkus.smallrye.reactivemessaging.runtime.devconsole.Connectors;
import io.quarkus.smallrye.reactivemessaging.runtime.devconsole.DevConsoleRecorder;
import io.quarkus.smallrye.reactivemessaging.runtime.devconsole.DevReactiveMessagingInfosSupplier;

public class ReactiveMessagingDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectInfos() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("reactiveMessagingInfos",
                new DevReactiveMessagingInfosSupplier());
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    public void collectInjectionInfo(DevConsoleRecorder recorder, BeanDiscoveryFinishedBuildItem beanDiscoveryFinished) {
        Map<String, String> emitters = new HashMap<>();
        Map<String, String> channels = new HashMap<>();
        for (InjectionPointInfo injectionPoint : beanDiscoveryFinished.getInjectionPoints()) {
            AnnotationInstance channelAnnotation = injectionPoint.getRequiredQualifier(ReactiveMessagingDotNames.CHANNEL);
            if (channelAnnotation == null) {
                channelAnnotation = injectionPoint.getRequiredQualifier(ReactiveMessagingDotNames.LEGACY_CHANNEL);
            }
            boolean isEmitter = injectionPoint.getRequiredType().name().equals(ReactiveMessagingDotNames.EMITTER)
                    || injectionPoint.getRequiredType().name()
                            .equals(ReactiveMessagingDotNames.MUTINY_EMITTER)
                    || injectionPoint.getRequiredType().name()
                            .equals(ReactiveMessagingDotNames.LEGACY_EMITTER);
            if (channelAnnotation != null) {
                if (isEmitter) {
                    emitters.put(channelAnnotation.value().asString(), injectionPoint.getTargetInfo());
                } else {
                    channels.put(channelAnnotation.value().asString(), injectionPoint.getTargetInfo());
                }
            }
        }
        recorder.setInjectionInfo(emitters, channels);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(Connectors.class);
    }

}
