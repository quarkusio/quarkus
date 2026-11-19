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
    private final Rule defaultRule;
    private final Map<String, GatewayConfig.Http.ParentRefConfig> parentRefs;
    private final List<Rule> extraRules;
    private final Map<String, String> annotations;
    private final boolean generateGateway;
    private final String gatewayName;

    public AddHttpRouteResourceDecorator(String name,
            List<String> hostnames,
            Rule defaultRule,
            Map<String, GatewayConfig.Http.ParentRefConfig> parentRefs,
            List<Rule> extraRules,
            Map<String, String> annotations,
            boolean generateGateway,
            String gatewayName) {
        this.name = name;
        this.hostnames = hostnames;
        this.defaultRule = defaultRule;
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
            for (GatewayConfig.Http.ParentRefConfig ref : parentRefs.values()) {
                ParentReferenceBuilder parentRefBuilder = new ParentReferenceBuilder()
                        .withName(ref.name())
                        .withKind(ref.kind())
                        .withGroup(ref.group());
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
        rules.add(buildRule(defaultRule));
        if (extraRules != null) {
            for (Rule rule : extraRules) {
                rules.add(buildRule(rule));
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

    private static HTTPRouteRule buildRule(Rule rule) {
        HTTPRouteRuleBuilder builder = new HTTPRouteRuleBuilder()
                .addNewMatch()
                .withNewPath()
                .withType(rule.pathType())
                .withValue(rule.path())
                .endPath()
                .endMatch()
                .addNewBackendRef()
                .withName(rule.serviceName())
                .withKind(SERVICE_KIND)
                .withPort(rule.port())
                .endBackendRef();
        rule.name().ifPresent(builder::withName);
        if (rule.requestTimeout().isPresent() || rule.backendRequestTimeout().isPresent()) {
            var timeouts = builder.withNewTimeouts();
            rule.requestTimeout().ifPresent(timeouts::withRequest);
            rule.backendRequestTimeout().ifPresent(timeouts::withBackendRequest);
            timeouts.endTimeouts();
        }
        return builder.build();
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

    public record Rule(
            Optional<String> name,
            String path,
            String pathType,
            String serviceName,
            int port,
            Optional<String> requestTimeout,
            Optional<String> backendRequestTimeout) {
    }
}
