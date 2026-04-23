package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpecFluent;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecFluent;

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
        extends BaseAddResourceDecorator<T, B, C> {
    private final Predicate<HasMetadata> toRemovePredicate;
    private final DeploymentResourceKind kind;

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
        super(name, config);
        this.kind = toAdd;
        if (toRemove != null && toRemove != toAdd) {
            toRemovePredicate = hm -> match(hm, toRemove.getApiVersion(), toRemove.getKind(), name);
        } else {
            toRemovePredicate = null;
        }
    }

    @Override
    protected void prepare(List<HasMetadata> items, KubernetesListBuilder list) {
        super.prepare(items, list);

        // remove other deployment kind if required
        if (toRemovePredicate != null) {
            for (HasMetadata item : items) {
                if (toRemovePredicate.test(item)) {
                    list.removeFromItems(item);
                    break;
                }
            }
        }
    }

    @Override
    protected String kind() {
        return kind.getKind();
    }

    @Override
    protected String apiVersion() {
        return kind.getApiVersion();
    }

    protected int replicas(Integer initialValue, ReplicasAware config) {
        int replicas = initialValue == null ? 1 : initialValue;
        if (config != null) {
            final var fromConfig = config.replicas();
            replicas = fromConfig != null && fromConfig != 1 ? fromConfig : replicas;
        }
        return replicas;
    }

    protected <PS extends PodSpecFluent<?>> PS podSpecDefaults(PS podSpec) {
        // termination grace period seconds
        if (podSpec.getTerminationGracePeriodSeconds() == null) {
            podSpec.withTerminationGracePeriodSeconds(10L);
        }

        // add container with deployment name
        if (!hasNamedContainer(podSpec)) {
            podSpec.addNewContainer().withName(name()).endContainer();
        }
        return podSpec;
    }

    private boolean hasNamedContainer(PodSpecFluent<?> spec) {
        List<Container> containers = spec.buildContainers();
        return containers != null && containers.stream().anyMatch(c -> name().equals(c.getName()));
    }

    protected void initFromConfig(JobSpecFluent<?> spec, JobConfig config) {
        spec.withCompletionMode(config.completionMode().name());
        spec.editTemplate().editSpec().withRestartPolicy(config.restartPolicy().name()).endSpec().endTemplate();
        config.parallelism().ifPresent(spec::withParallelism);
        config.completions().ifPresent(spec::withCompletions);
        config.backoffLimit().ifPresent(spec::withBackoffLimit);
        config.activeDeadlineSeconds().ifPresent(spec::withActiveDeadlineSeconds);
        config.ttlSecondsAfterFinished().ifPresent(spec::withTtlSecondsAfterFinished);
    }
}
