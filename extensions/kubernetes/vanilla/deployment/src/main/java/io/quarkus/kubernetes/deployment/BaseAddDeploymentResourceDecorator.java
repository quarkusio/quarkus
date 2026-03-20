package io.quarkus.kubernetes.deployment;

import java.util.Optional;
import java.util.function.Predicate;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.builder.BaseFluent;
import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListFluent;

/**
 * Base class for decorators handling the generation of deployment resources as identified by {@link DeploymentResourceKind},
 * optionally removing a specified default deployment resource that might have been added by default for a given manifest type,
 * e.g. {@link DeploymentResourceKind#DeploymentConfig} for an OpenShift manifest when we want to replace this default one by
 * another deployment type, such as a plain Deployment.
 *
 * @param <T> the type of deployment resource to create e.g. {@link io.fabric8.kubernetes.api.model.apps.Deployment}
 * @param <B> the {@link VisitableBuilder} type associated with the resource
 * @param <C> an optional configuration type that can be used to populate the resource's field, e.g. {@link JobConfig} or
 *        {@link Void} if such configuration is not needed
 */
abstract class BaseAddDeploymentResourceDecorator<T extends HasMetadata, B extends VisitableBuilder<T, B>, C>
        extends ResourceProvidingDecorator<KubernetesListFluent<?>> {
    private final Predicate<HasMetadata> toRemovePredicate;
    private final DeploymentResourceKind kind;
    private final String name;
    private final C config;

    /**
     * Create a new decorator to handle deployment-like resources, optionally handling the removal of the default deployment
     * resource for the target platform.
     *
     * @param name the name of the resource to add
     * @param toAdd the type of deployment-like resource to add
     * @param config the optional configuration associated with the deployment resource, {@code null} otherwise
     * @param toRemove the optional resource type to remove or {@code null} if no such resource needs to be removed. Note that
     *        this will remove all (presumably only one) resources with the kind associated with this parameter and the
     *        specified name.
     */
    public BaseAddDeploymentResourceDecorator(String name, DeploymentResourceKind toAdd, C config,
            DeploymentResourceKind toRemove) {
        if (toRemove != null && toRemove != toAdd) {
            toRemovePredicate = hm -> hm.getKind().equals(toRemove.getKind())
                    && hm.getMetadata().getName().equalsIgnoreCase(name);
        } else {
            toRemovePredicate = null;
        }
        this.name = name;
        this.kind = toAdd;
        this.config = config;
    }

    @Override
    public void visit(KubernetesListFluent<?> list) {
        // remove other deployment kind if required
        final var items = list.buildItems();
        if (toRemovePredicate != null) {
            items.removeIf(toRemovePredicate);
        }

        // replace or create the desired deployment resource
        Optional<B> found = Optional.empty();
        for (HasMetadata hasMetadata : items) {
            if (existing(hasMetadata)) {
                list.removeFromItems(hasMetadata);
                @SuppressWarnings("unchecked")
                B apply = (B) BaseFluent.builderOf((T) hasMetadata);
                found = Optional.of(apply);
                break;
            }
        }
        final var builder = found.orElseGet(() -> builderWithName(name));
        initBuilderWithDefaults(builder, config);

        // add it to generated items list
        list.addToItems(builder.build());
    }

    protected String name() {
        return name;
    }

    private boolean existing(HasMetadata hasMetadata) {
        return kind.getKind().equalsIgnoreCase(hasMetadata.getKind()) && name.equals(hasMetadata.getMetadata().getName());
    }

    protected abstract B builderWithName(String name);

    protected abstract void initBuilderWithDefaults(B builder, C config);
}
