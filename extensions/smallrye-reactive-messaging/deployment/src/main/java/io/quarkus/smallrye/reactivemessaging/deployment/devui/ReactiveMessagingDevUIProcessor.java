package io.quarkus.smallrye.reactivemessaging.deployment.devui;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames;
import io.quarkus.smallrye.reactivemessaging.runtime.dev.ui.Connectors;
import io.quarkus.smallrye.reactivemessaging.runtime.dev.ui.DevConsoleRecorder;
import io.quarkus.smallrye.reactivemessaging.runtime.dev.ui.ReactiveMessagingJsonRpcService;

public class ReactiveMessagingDevUIProcessor {

    @io.quarkus.deployment.annotations.Record(STATIC_INIT)
    @BuildStep(onlyIf = IsLocalDevelopment.class)
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

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(Connectors.class);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem create() {
        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.webComponentPageBuilder()
                .title("Channels")
                .componentLink("qwc-smallrye-reactive-messaging-channels.js")
                .icon("font-awesome-solid:diagram-project"));

        return card;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(ReactiveMessagingJsonRpcService.class);
    }
}
