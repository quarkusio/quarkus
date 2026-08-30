package io.quarkus.kubernetes.deployment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddIngressDecorator;
import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpecBuilder;

public class ApplyJaxRsIngressRulesDecorator extends NamedResourceDecorator<IngressSpecBuilder> {

    private final List<String> paths;
    private final Optional<Port> defaultPort;
    private final String host;

    public ApplyJaxRsIngressRulesDecorator(String name, List<String> paths, Optional<Port> defaultPort, String host) {
        super(name);
        this.paths = paths;
        this.defaultPort = defaultPort;
        this.host = host;
    }

    @Override
    public void andThenVisit(IngressSpecBuilder spec, ObjectMeta resourceMeta) {
        if (spec.hasMatchingRule(r -> Objects.equals(host, r.getHost()))) {
            spec.editMatchingRule(r -> Objects.equals(host, r.getHost()))
                    .withNewHttp()
                    .addAllToPaths(buildPaths())
                    .endHttp()
                    .endRule();
        } else {
            spec.addNewRule()
                    .withHost(host)
                    .withNewHttp()
                    .addAllToPaths(buildPaths())
                    .endHttp()
                    .endRule();
        }
    }

    private List<HTTPIngressPath> buildPaths() {
        Map<String, HTTPIngressPath> deduped = new LinkedHashMap<>();
        for (String path : paths) {
            String finalPath = JaxRsPathCollector.hasPathTemplate(path)
                    ? JaxRsPathCollector.staticPrefix(path)
                    : path;
            deduped.putIfAbsent(finalPath, buildPath(path));
        }
        return new ArrayList<>(deduped.values());
    }

    private HTTPIngressPath buildPath(String path) {
        boolean hasTemplate = JaxRsPathCollector.hasPathTemplate(path);
        String finalPath = hasTemplate ? JaxRsPathCollector.staticPrefix(path) : path;
        String pathType = hasTemplate ? "Prefix" : "Exact";

        String portName = defaultPort.map(Port::getName).orElse("http");

        return new HTTPIngressPathBuilder()
                .withPath(finalPath)
                .withPathType(pathType)
                .withNewBackend()
                .withNewService()
                .withName(name)
                .withNewPort()
                .withName(portName)
                .endPort()
                .endService()
                .endBackend()
                .build();
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddIngressDecorator.class, AddIngressRuleDecorator.class };
    }
}
