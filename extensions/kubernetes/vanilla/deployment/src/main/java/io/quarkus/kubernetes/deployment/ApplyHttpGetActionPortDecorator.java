package io.quarkus.kubernetes.deployment;

import static io.dekorate.ConfigReference.joinProperties;
import static io.dekorate.utils.Metadata.getMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.ConfigReference;
import io.dekorate.WithConfigReferences;
import io.dekorate.kubernetes.decorator.AbstractAddProbeDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.builder.Builder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionFluent;

public class ApplyHttpGetActionPortDecorator extends Decorator<HTTPGetActionFluent<?>> implements WithConfigReferences {

    private static final String PATH_ALL_EXPRESSION = "*.spec.containers.";
    private static final String PATH_DEPLOYMENT_CONTAINER_EXPRESSION = "(metadata.name == %s).spec.template.spec.containers.(name == %s).";
    private static final String PATH_DEPLOYMENT_EXPRESSION = "(metadata.name == %s).spec.template.spec.containers.";
    private static final String PATH_CONTAINER_EXPRESSION = "*.spec.containers.(name == %s).";

    private final String deployment;
    private final String container;
    private final String portName;
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
        this(deployment, container, null, port, probeKind, port != null && (port == 443 || port == 8443) ? "HTTPS" : "HTTP"); // this is the original convention coming from dekorate
    }

    public ApplyHttpGetActionPortDecorator(String deployment, String container, String portName, Integer port, String probeKind,
            String scheme) {
        this.deployment = deployment;
        this.container = container;
        this.portName = portName;
        this.port = port;
        this.probeKind = probeKind;
        this.scheme = scheme;
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
            action.withNewPort(null);
        } else {
            action.withNewPort(port);
        }

        if (scheme == null) {
            action.withScheme(null);
        } else {
            action.withScheme(scheme);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class, AbstractAddProbeDecorator.class };
    }

    @Override
    public List<ConfigReference> getConfigReferences() {
        if (portName != null && probeKind != null) {
            return List.of(buildConfigReference(joinProperties("ports." + portName),
                    "httpGet.port", port, "The http port to use for the probe."));
        }

        return Collections.emptyList();
    }

    private ConfigReference buildConfigReference(String property, String probeField, Object value, String description) {
        String expression = PATH_ALL_EXPRESSION;
        if (Strings.isNotNullOrEmpty(deployment) && Strings.isNotNullOrEmpty(container)) {
            expression = String.format(PATH_DEPLOYMENT_CONTAINER_EXPRESSION, deployment, container);
        } else if (Strings.isNotNullOrEmpty(deployment)) {
            expression = String.format(PATH_DEPLOYMENT_EXPRESSION, deployment);
        } else if (Strings.isNotNullOrEmpty(container)) {
            expression = String.format(PATH_CONTAINER_EXPRESSION, container);
        }

        String yamlPath = expression + probeKind + "." + probeField;
        return new ConfigReference.Builder(property, yamlPath).withDescription(description).withValue(value).build();
    }
}
