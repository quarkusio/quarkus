package io.quarkus.kubernetes.deployment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import io.dekorate.kubernetes.decorator.AddIngressDecorator;
import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpecFluent;

/**
 * Sets the path type of the Ingress rule that is generated for the application host and the path of the target
 * port. The generated rule is created without a path type, which Kubernetes and Dekorate read as {@code Prefix}, and
 * a user rule that only differs in its path type is appended next to it instead of replacing it. After the path type
 * is applied, paths that became identical are collapsed into one.
 */
public class ApplyIngressPathTypeDecorator extends NamedResourceDecorator<IngressSpecFluent<?>> {

    private final String host;
    private final String path;
    private final String pathType;

    public ApplyIngressPathTypeDecorator(String name, String host, String path, String pathType) {
        super(name);
        this.host = host;
        this.path = path;
        this.pathType = pathType;
    }

    @Override
    public void andThenVisit(IngressSpecFluent<?> spec, ObjectMeta resourceMeta) {
        List<IngressRule> rules = new ArrayList<>();
        for (IngressRule rule : spec.buildRules()) {
            if (matchesHost(rule) && rule.getHttp() != null && rule.getHttp().getPaths() != null) {
                for (HTTPIngressPath ingressPath : rule.getHttp().getPaths()) {
                    if (path.equals(ingressPath.getPath())) {
                        ingressPath.setPathType(pathType);
                    }
                }
                rule.getHttp().setPaths(new ArrayList<>(new LinkedHashSet<>(rule.getHttp().getPaths())));
            }
            rules.add(rule);
        }
        spec.withRules(rules);
    }

    private boolean matchesHost(IngressRule rule) {
        return host == null ? rule.getHost() == null : host.equals(rule.getHost());
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddIngressDecorator.class, AddIngressRuleDecorator.class };
    }
}
