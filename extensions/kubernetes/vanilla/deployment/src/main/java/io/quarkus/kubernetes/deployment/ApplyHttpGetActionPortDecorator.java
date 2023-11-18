package io.quarkus.kubernetes.deployment;

import static io.dekorate.utils.Metadata.getMetadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.decorator.AbstractAddProbeDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Metadata;
import io.fabric8.kubernetes.api.builder.Builder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionFluent;

public class ApplyHttpGetActionPortDecorator extends Decorator<HTTPGetActionFluent<?>> {

    private final String deployment;
    private final String container;
    private final Integer port;
    private final String scheme;
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
        this(deployment, container, port, probeKind, port != null && (port == 443 || port == 8443) ? "HTTPS" : "HTTP"); // this is the original convention coming from dekorate
    }

    public ApplyHttpGetActionPortDecorator(String deployment, String container, Integer port, String probeKind, String scheme) {
        this.deployment = deployment;
        this.container = container;
        this.port = port;
        this.probeKind = probeKind;
        this.scheme = scheme;
    }

    @Override
    public void visit(List<Map.Entry<String, Object>> path, HTTPGetActionFluent<?> action) {
        boolean inMatchingProbe = probeKind == ANY || path.stream().map(Map.Entry::getKey).anyMatch(i -> i.equals(probeKind));
        if (!inMatchingProbe) {
            return;
        }

        boolean inMatchingContainer = container == ANY || path.stream()
                .map(Map.Entry::getValue)
                .filter(ContainerBuilder.class::isInstance)
                .map(v -> (ContainerBuilder) v)
                .anyMatch(c -> c.getName() != null && c.getName().equals(container));

        if (!inMatchingContainer) {
            return;
        }

        boolean inMatchingResource = deployment == ANY || path.stream()
                .map(Map.Entry::getValue)
                .filter(Builder.class::isInstance)
                .map(v -> (Builder) v)
                .map(Metadata::getMetadata)
                .filter(Optional::isPresent)
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

        if (scheme == null) {
            action.withScheme((String) null);
        } else {
            action.withScheme(scheme);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class, AbstractAddProbeDecorator.class };
    }
}
