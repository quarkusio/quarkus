package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpecBuilder;

/**
 * TODO: Workaround for https://github.com/quarkusio/quarkus/issues/28812
 * We need to remove the duplicate paths of the generated Ingress. The following logic can be removed after
 * bumping the next Dekorate version that includes the fix: https://github.com/dekorateio/dekorate/pull/1092.
 */
public class RemoveDuplicateIngressRuleDecorator extends NamedResourceDecorator<IngressSpecBuilder> {

    public RemoveDuplicateIngressRuleDecorator(String name) {
        super(name);
    }

    @Override
    public void andThenVisit(IngressSpecBuilder spec, ObjectMeta meta) {
        if (spec.hasRules()) {
            spec.editMatchingRule(rule -> {
                rule.editHttp()
                        .removeMatchingFromPaths(path -> rule.getHttp().getPaths().stream()
                                .filter(p -> p.hashCode() == path.hashCode()).count() > 1)
                        .endHttp();
                return true;
            });
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddIngressRuleDecorator.class };
    }
}
