package io.quarkus.kubernetes.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.builder.BaseFluent;
import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorFluent;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;

/**
 * Base class for decorators handling the generation of Kubernetes resources. This takes care of identifying, if any, matching
 * resources that might have been manually provided by users and potentially change values in these resources based on an
 * optional configuration.
 *
 * @param <T> the type of resource to create e.g. {@link io.fabric8.kubernetes.api.model.apps.Deployment}
 * @param <B> the {@link VisitableBuilder} type associated with the resource
 * @param <C> an optional configuration type that can be used to populate the resource's field, e.g. {@link JobConfig} or
 *        {@link Void} if such configuration is not needed
 */
abstract class BaseAddResourceDecorator<T extends HasMetadata, B extends VisitableBuilder<T, B>, C>
        extends ResourceProvidingDecorator<KubernetesListBuilder> {
    private static final List<String> DEPLOYMENT_KINDS = List.of("Deployment", "DeploymentConfig", "Service", "StatefulSet",
            "Pipeline", "Task", "Job", "CronJob");

    private final String name;
    private final C config;

    /**
     * Create a new decorator to add a new named resource .
     *
     * @param name the name of the resource to add
     * @param config the optional configuration associated with the deployment resource, {@code null} otherwise
     */
    public BaseAddResourceDecorator(String name, C config) {
        this.name = name;
        this.config = config;
    }

    public BaseAddResourceDecorator(String name) {
        this(name, null);
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        // generate the resources so that we can do matching on them
        final var items = list.buildItems();

        prepare(items, list);

        Optional<B> found = Optional.empty();
        for (HasMetadata hasMetadata : items) {
            if (match(hasMetadata)) {
                list.removeFromItems(hasMetadata);
                @SuppressWarnings("unchecked")
                B builder = (B) BaseFluent.builderOf((T) hasMetadata);
                found = Optional.of(builder);
                break;
            }
        }
        final var builder = found.orElseGet(() -> builderWithName(name));
        initBuilderWithDefaults(builder, config);

        // add it to generated items list
        list.addToItems(builder.build());
    }

    protected boolean match(HasMetadata hasMetadata) {
        return match(hasMetadata, apiVersion(), kind(), name);
    }

    /**
     * Retrieves the name of the associated resource to be added
     *
     * @return the name of the resource to be added by this decorator
     */
    protected String name() {
        return name;
    }

    protected void prepare(List<HasMetadata> items, KubernetesListBuilder list) {
        // nothing to do by default
    }

    /**
     * Creates a resource builder {@code T} resources that will be used to create the resource to add
     *
     * @param name the name of the resource to create
     * @return a {@code T} resource builder already initialized with the future resource name
     */
    protected abstract B builderWithName(String name);

    /**
     * Initializes the specified resource builder either from hard-coded defaults or from the optional, potentially {@code null}
     * provided configuration
     *
     * @param builder the resource builder that is used to create the resource to add and that needs to be initialized
     * @param config an optional configuration object to retrieve values from to init the builder with
     */
    protected abstract void initBuilderWithDefaults(B builder, C config);

    /**
     * Returns the api version of resources being handled by this decorator
     *
     * @return the api version of resources being handled by this decorator
     */
    protected abstract String apiVersion();

    /**
     * Returns the kind of resources being handled by this decorator
     *
     * @return the kind of resources being handled by this decorator
     */
    protected abstract String kind();

    /**
     * Returns the first deployment-like resource metadata with the given name if it exists in the specified list of items. Note
     * that we're using a method derived from dekorate but more optimized and avoiding rebuilding the list of items that we
     * already have available.
     *
     * @param items the list of items to search
     * @param name the name of the deployment-like resource to search for
     * @return the found deployment-like resource or {@link Optional#empty()}
     */
    protected static Optional<ObjectMeta> deploymentMetadataNamed(List<HasMetadata> items, @Nullable String name) {
        // adapted from ResourceProvidingDecorator.getDeploymentMetadata method
        // In 99% of the cases we select metadata by name.
        // There are some edge cases (e.g. RoleBindings) where a suffix is added (e.g. <name>:deployer).
        // We need to get rid of such suffixes when present as we NEVER have them in `deployment` kinds.
        if (name != null && !name.isBlank()) {
            final var columnIndex = name.indexOf(':');
            if (columnIndex > 0) {
                name = name.substring(0, columnIndex);
            }
        }
        final String trimmed = name;

        return items
                .stream()
                .filter(h -> DEPLOYMENT_KINDS.contains(h.getKind()))
                .map(HasMetadata::getMetadata)
                .filter(metadata -> Objects.equals(trimmed, ANY) || metadata.getName().equals(trimmed))
                .findFirst();
    }

    /**
     * Updates the metadata for the builder as required. Don't forget to call {@code endMetadata()} on the returned value!
     *
     * @param builder the metadata builder extracted from this decorator's main builder (as specified by the {@code B} type
     *        parameter
     * @param namespace a potentially {@code null} namespace to set on the metadata
     * @param <O> the metadata data builder type (typically, a {@code MetadataNested} instance provided by calling
     *        {@code editOrNewMetadata()} on the parent builder
     * @return the metadata builder that was passed as first parameter
     */
    protected <O extends ObjectMetaFluent<?>> O updateMetadata(O builder, @Nullable String namespace,
            Map<String, String> labels) {
        if (namespace != null) {
            builder.withNamespace(namespace);
        }
        builder.addToLabels(labels);
        return builder;
    }

    protected Map<String, String> mergeLabelsFromDeploymentWith(List<HasMetadata> items, Map<String, String> labels,
            @Nullable String deploymentName) {
        Map<String, String> merged = new HashMap<>(labels);
        deploymentMetadataNamed(items, deploymentName)
                .map(ObjectMeta::getLabels)
                .ifPresent(merged::putAll);
        return merged;
    }

    protected <S extends LabelSelectorFluent<?>> S initMatchLabels(S selector) {
        if (!selector.hasMatchLabels()) {
            selector.withMatchLabels(new HashMap<>());
        }
        return selector;
    }
}
