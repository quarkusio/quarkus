package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.GatewayBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.GatewayFluent;

public class AddGatewayResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private final String name;
    private final String gatewayClassName;
    private final Map<String, GatewayConfig.ListenerConfig> listeners;
    private final Map<String, String> annotations;
    private final Optional<String> defaultHostname;

    public AddGatewayResourceDecorator(String name, String gatewayClassName,
            Map<String, GatewayConfig.ListenerConfig> listeners,
            Map<String, String> annotations,
            Optional<String> defaultHostname) {
        this.name = name;
        this.gatewayClassName = gatewayClassName;
        this.listeners = listeners;
        this.annotations = annotations;
        this.defaultHostname = defaultHostname;
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        ObjectMeta meta = getMandatoryDeploymentMetadata(list, ANY);

        GatewayBuilder builder = new GatewayBuilder();
        GatewayFluent<?>.MetadataNested<?> metadata = builder.withNewMetadata()
                .withName(name)
                .withLabels(meta.getLabels());
        if (annotations != null && !annotations.isEmpty()) {
            metadata.addToAnnotations(annotations);
        }
        metadata.endMetadata();

        var spec = builder.withNewSpec()
                .withGatewayClassName(gatewayClassName);

        if (listeners == null || listeners.isEmpty()) {
            var listener = spec.addNewListener()
                    .withName("http")
                    .withProtocol("HTTP")
                    .withPort(80);
            defaultHostname.ifPresent(listener::withHostname);
            listener.endListener();
        } else {
            for (GatewayConfig.ListenerConfig listenerConfig : listeners.values()) {
                var listener = spec.addNewListener()
                        .withName(listenerConfig.name())
                        .withProtocol(listenerConfig.protocol())
                        .withPort(listenerConfig.port());
                listenerConfig.hostname().ifPresent(listener::withHostname);
                listener.endListener();
            }
        }

        spec.endSpec();
        list.addToItems(builder.build());
    }
}
