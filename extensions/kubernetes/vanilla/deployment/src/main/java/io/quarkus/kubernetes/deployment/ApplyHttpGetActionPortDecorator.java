package io.quarkus.kubernetes.deployment;

import static io.dekorate.utils.Metadata.getMetadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.decorator.AddLivenessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddReadinessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.builder.Builder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionFluent;

public class ApplyHttpGetActionPortDecorator extends Decorator<HTTPGetActionFluent<?>> {

    private final String deployment;
    private final String container;
    private final Integer port;
    private final String probeKind;

    public ApplyHttpGetActionPortDecorator(Integer port) {
        this(ANY, ANY, port, ANY);
    }

    public ApplyHttpGetActionPortDecorator(Integer port, String probeKind) {
        this(ANY, ANY, port, probeKind);
    }

    public ApplyHttpGetActionPortDecorator(String deployment, Integer port) {
        this(deployment, ANY, port, ANY);
    }

    public ApplyHttpGetActionPortDecorator(String deployment, Integer port, String probeKind) {
        this(deployment, ANY, port, probeKind);
    }

    public ApplyHttpGetActionPortDecorator(String deployment, String container, Integer port) {
        this(deployment, container, port, ANY);
    }

    public ApplyHttpGetActionPortDecorator(String deployment, String container, Integer port, String probeKind) {
        this.deployment = deployment;
        this.container = container;
        this.port = port;
        this.probeKind = probeKind;
    }

    @Override
    public void visit(List<Map.Entry<String, Object>> path, HTTPGetActionFluent<?> action) {
        boolean inMatchingProbe = probeKind == ANY || path.stream().map(e -> e.getKey()).anyMatch(i -> i.equals(probeKind));
        if (!inMatchingProbe) {
            return;
        }

        boolean inMatchingContainer = container == ANY || path.stream()
                .map(e -> e.getValue())
                .filter(v -> v instanceof ContainerBuilder)
                .map(v -> (ContainerBuilder) v)
                .anyMatch(c -> c.getName() != null && c.getName().equals(container));

        if (!inMatchingContainer) {
            return;
        }

        boolean inMatchingResource = deployment == ANY || path.stream()
                .map(e -> e.getValue())
                .filter(v -> v instanceof Builder)
                .map(v -> (Builder) v)
                .map(b -> getMetadata(b))
                .filter(m -> m.isPresent())
                .map(Optional::get)
                .anyMatch(m -> m.getName() != null && m.getName().equals(deployment));

        if (!inMatchingResource) {
            return;
        }

        visit(action);
    }

    @Override
    public void visit(HTTPGetActionFluent<?> action) {
        if (port == null) {
            // workaround to make sure we don't get a NPE
            action.withNewPort((String) null);
        } else {
            action.withNewPort(port);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class,
                AddLivenessProbeDecorator.class, AddReadinessProbeDecorator.class };
    }
}
