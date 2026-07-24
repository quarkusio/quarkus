package io.quarkus.kubernetes.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteRule;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteRuleBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.ParentReference;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.ParentReferenceBuilder;

public class AddHttpRouteResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private static final String GATEWAY_API_GROUP = "gateway.networking.k8s.io";
    private static final String GATEWAY_KIND = "Gateway";
    private static final String SERVICE_KIND = "Service";

    private final String name;
    private final List<String> hostnames;
    private final String path;
    private final String pathType;
    private final String backendServiceName;
    private final int backendPort;
    private final Map<String, GatewayConfig.ParentRefConfig> parentRefs;
    private final List<Rule> extraRules;
    private final Map<String, String> annotations;
    private final boolean generateGateway;
    private final String gatewayName;

    public AddHttpRouteResourceDecorator(String name,
            List<String> hostnames,
            String path,
            String pathType,
            String backendServiceName,
            int backendPort,
            Map<String, GatewayConfig.ParentRefConfig> parentRefs,
            List<Rule> extraRules,
            Map<String, String> annotations,
            boolean generateGateway,
            String gatewayName) {
        this.name = name;
        this.hostnames = hostnames;
        this.path = path;
        this.pathType = pathType;
        this.backendServiceName = backendServiceName;
        this.backendPort = backendPort;
        this.parentRefs = parentRefs;
        this.extraRules = extraRules;
        this.annotations = annotations;
        this.generateGateway = generateGateway;
        this.gatewayName = gatewayName;
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        ObjectMeta meta = getMandatoryDeploymentMetadata(list, ANY);

        List<ParentReference> resolvedParentRefs = new ArrayList<>();
        if (parentRefs != null && !parentRefs.isEmpty()) {
            for (GatewayConfig.ParentRefConfig ref : parentRefs.values()) {
                ParentReferenceBuilder parentRefBuilder = new ParentReferenceBuilder()
                        .withName(ref.name())
                        .withKind(ref.kind().orElse(GATEWAY_KIND))
                        .withGroup(ref.group().orElse(GATEWAY_API_GROUP));
                ref.namespace().ifPresent(parentRefBuilder::withNamespace);
                ref.sectionName().ifPresent(parentRefBuilder::withSectionName);
                resolvedParentRefs.add(parentRefBuilder.build());
            }
        } else if (generateGateway) {
            resolvedParentRefs.add(new ParentReferenceBuilder()
                    .withName(gatewayName)
                    .withKind(GATEWAY_KIND)
                    .withGroup(GATEWAY_API_GROUP)
                    .build());
        }

        List<HTTPRouteRule> rules = new ArrayList<>();
        rules.add(buildRule(path, pathType, backendServiceName, backendPort));
        if (extraRules != null) {
            for (Rule rule : extraRules) {
                rules.add(buildRule(rule.path(), rule.pathType(), rule.serviceName(), rule.port()));
            }
        }

        HTTPRouteBuilder builder = new HTTPRouteBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(meta.getLabels())
                .endMetadata()
                .withNewSpec()
                .withParentRefs(resolvedParentRefs)
                .withRules(rules)
                .endSpec();

        if (annotations != null && !annotations.isEmpty()) {
            builder.editMetadata().addToAnnotations(annotations).endMetadata();
        }
        if (hostnames != null && !hostnames.isEmpty()) {
            builder.editSpec().withHostnames(hostnames).endSpec();
        }

        HTTPRoute route = builder.build();
        list.addToItems(route);
    }

    private static HTTPRouteRule buildRule(String path, String pathType, String serviceName, int port) {
        return new HTTPRouteRuleBuilder()
                .addNewMatch()
                .withNewPath()
                .withType(pathType)
                .withValue(path)
                .endPath()
                .endMatch()
                .addNewBackendRef()
                .withName(serviceName)
                .withKind(SERVICE_KIND)
                .withPort(port)
                .endBackendRef()
                .build();
    }

    public static List<String> combineHostnames(Optional<String> host, Optional<List<String>> hosts) {
        List<String> result = new ArrayList<>();
        host.ifPresent(result::add);
        hosts.ifPresent(list -> {
            for (String h : list) {
                if (!result.contains(h)) {
                    result.add(h);
                }
            }
        });
        return result;
    }

    public record Rule(String path, String pathType, String serviceName, int port) {
    }
}
