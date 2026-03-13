package io.quarkus.aesh.websocket.deployment;

import jakarta.enterprise.inject.Vetoed;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.aesh.deployment.AeshRemoteTransportBuildItem;
import io.quarkus.aesh.websocket.runtime.AeshWebSocketConfig;
import io.quarkus.aesh.websocket.runtime.AeshWebSocketEndpoint;
import io.quarkus.aesh.websocket.runtime.AeshWebSocketRecorder;
import io.quarkus.aesh.websocket.runtime.AeshWebSocketSecurityCheck;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.WebSocket;

class AeshWebSocketProcessor {

    private static final Logger LOG = Logger.getLogger(AeshWebSocketProcessor.class);

    private static final DotName ENDPOINT_CLASS = DotName.createSimple(AeshWebSocketEndpoint.class);
    private static final DotName HEALTH_CHECK_CLASS = DotName
            .createSimple("io.quarkus.aesh.websocket.runtime.health.AeshWebSocketHealthCheck");
    private static final DotName WEB_SOCKET = DotName.createSimple(WebSocket.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AESH_WEBSOCKET);
    }

    @BuildStep
    void registerBeans(AeshWebSocketConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        if (config.enabled()) {
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(AeshWebSocketEndpoint.class));
        }
    }

    @BuildStep
    void remoteTransport(AeshWebSocketConfig config,
            BuildProducer<AeshRemoteTransportBuildItem> producer) {
        if (config.enabled()) {
            producer.produce(new AeshRemoteTransportBuildItem("websocket"));
        }
    }

    @BuildStep
    void disableEndpointIfNotEnabled(AeshWebSocketConfig config,
            BuildProducer<AnnotationsTransformerBuildItem> transformers) {
        if (!config.enabled()) {
            transformers.produce(new AnnotationsTransformerBuildItem(
                    AnnotationTransformation.forClasses()
                            .whenClass(c -> c.name().equals(ENDPOINT_CLASS)
                                    || c.name().equals(HEALTH_CHECK_CLASS))
                            .transform(ctx -> ctx.add(Vetoed.class))));
        } else if (!config.healthEnabled()) {
            transformers.produce(new AnnotationsTransformerBuildItem(
                    AnnotationTransformation.forClasses()
                            .whenClass(c -> c.name().equals(HEALTH_CHECK_CLASS))
                            .transform(ctx -> ctx.add(Vetoed.class))));
        }
    }

    @BuildStep
    void configureWebSocketPath(AeshWebSocketConfig config,
            BuildProducer<AnnotationsTransformerBuildItem> transformers) {
        if (!config.enabled()) {
            return;
        }
        String path = config.path();
        transformers.produce(new AnnotationsTransformerBuildItem(
                AnnotationTransformation.forClasses()
                        .whenClass(c -> c.name().equals(ENDPOINT_CLASS))
                        .transform(ctx -> {
                            ctx.remove(ai -> ai.name().equals(WEB_SOCKET));
                            ctx.add(AnnotationInstance.create(WEB_SOCKET, ctx.declaration(),
                                    new AnnotationValue[] {
                                            AnnotationValue.createStringValue("path", path)
                                    }));
                        })));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordWebSocketPath(AeshWebSocketConfig config, AeshWebSocketRecorder recorder) {
        if (config.enabled()) {
            recorder.setWebSocketPath(config.path());
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void addSecurityCheck(AeshWebSocketConfig config,
            Capabilities capabilities,
            AeshWebSocketRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        if (!config.enabled()) {
            return;
        }
        boolean hasRolesAllowed = config.rolesAllowed().isPresent() && !config.rolesAllowed().get().isEmpty();
        if (!hasRolesAllowed && !config.authenticated()) {
            return;
        }
        if (!capabilities.isPresent(Capability.SECURITY)) {
            LOG.warn("Aesh WebSocket security configuration requires a Quarkus Security extension " +
                    "(e.g. quarkus-elytron-security-properties-file) but none was found. " +
                    "Authentication will not be enforced.");
            return;
        }

        syntheticBeans.produce(SyntheticBeanBuildItem
                .configure(AeshWebSocketSecurityCheck.class)
                .types(HttpUpgradeCheck.class)
                .defaultBean()
                .unremovable()
                .supplier(recorder.createSecurityCheck(
                        hasRolesAllowed ? config.rolesAllowed().get() : null,
                        config.authenticated()))
                .done());
    }

    @BuildStep
    HealthBuildItem addHealthCheck(AeshWebSocketConfig config) {
        return new HealthBuildItem(
                "io.quarkus.aesh.websocket.runtime.health.AeshWebSocketHealthCheck",
                config.enabled() && config.healthEnabled());
    }

    @BuildStep
    @Produce(ArtifactResultBuildItem.class)
    void warnIfInsecureInProduction(AeshWebSocketConfig config,
            LaunchModeBuildItem launchMode) {
        if (config.enabled() && launchMode.getLaunchMode() == io.quarkus.runtime.LaunchMode.NORMAL) {
            if (config.rolesAllowed().isEmpty() && !config.authenticated()) {
                LOG.warn("Aesh WebSocket terminal is enabled in production without " +
                        "authentication. Set 'quarkus.aesh.websocket.roles-allowed' or " +
                        "'quarkus.aesh.websocket.authenticated=true' to secure the endpoint.");
            }
        }
    }
}
