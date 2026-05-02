package io.quarkus.kubernetes.deployment;

import java.util.function.Predicate;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpecFluent;

public class ApplySecuritySettingsDecorator extends NamedResourceDecorator<PodSpecFluent<?>> {

    private final SecurityContextConfig securityContext;
    private final Predicate<ContainerBuilder> hasNamedContainer = cb -> cb.getName().equals(name);

    public ApplySecuritySettingsDecorator(String resourceName, SecurityContextConfig securityContext) {
        super(resourceName);
        this.securityContext = securityContext;
    }

    @Override
    public void andThenVisit(PodSpecFluent<?> podSpec, ObjectMeta resourceMeta) {
        podSpec.withSecurityContext(securityContext.buildSecurityContext());

        // configure application container with security options if present
        final var maybeReadOnly = securityContext.readOnlyRootFilesystem();
        final var maybeEscalation = securityContext.allowPrivilegeEscalation();
        if (maybeReadOnly.isPresent() || maybeEscalation.isPresent()) {
            // create container if absent
            if (!podSpec.hasMatchingContainer(hasNamedContainer)) {
                podSpec.addNewContainer().withName(name).endContainer();
            }

            final var containerSecContext = podSpec.editMatchingContainer(hasNamedContainer)
                    .editOrNewSecurityContext();
            maybeReadOnly.ifPresent(containerSecContext::withReadOnlyRootFilesystem);
            maybeEscalation.ifPresent(containerSecContext::withAllowPrivilegeEscalation);
            containerSecContext.endSecurityContext().endContainer();
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }
}
