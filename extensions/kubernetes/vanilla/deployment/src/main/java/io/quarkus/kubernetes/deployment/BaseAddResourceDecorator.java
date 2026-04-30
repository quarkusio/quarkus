package io.quarkus.kubernetes.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.builder.BaseFluent;
import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorFluent;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;
import io.fabric8.kubernetes.api.model.PodSpecFluent;

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
    private final String apiVersion;
    private final String kind;

    /**
     * Create a new decorator to add a new named resource .
     *
     * @param name the name of the resource to add
     * @param config the optional configuration associated with the deployment resource, {@code null} otherwise
     */
    public BaseAddResourceDecorator(String name, String kind, String apiVersion, C config) {
        this.name = name;
        this.config = config;
        this.apiVersion = apiVersion;
        this.kind = kind;
    }

    public BaseAddResourceDecorator(String name, String kind, String apiVersion) {
        this(name, kind, apiVersion, null);
    }

    public BaseAddResourceDecorator(String name, Class<? extends HasMetadata> resourceClass, C config) {
        this(name, HasMetadata.getKind(resourceClass), HasMetadata.getApiVersion(resourceClass), config);
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        // generate the resources so that we can do matching on them
        final var items = list.buildItems();

        addOrEditExisting(items, list);
    }

    /**
     * Adds the target item to the specified {@link KubernetesListBuilder} if needed, from an already built items list. This is
     * useful to replicate the decorator behavior without actually using a decorator and without requiring a list rebuilding.
     *
     * @param items the built items list
     * @param list the builder list that we want to modify
     */
    protected void addOrEditExisting(List<HasMetadata> items, KubernetesListBuilder list) {
        // perform items-wide operations if needed
        prepare(items, list);

        // remove existing target item if existing and "open" it for editing
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
        // otherwise create a new builder
        final var builder = found.orElseGet(() -> builderWithName(name));
        // and init it with defaults
        initBuilderWithDefaults(builder);

        // add it to generated items list
        list.addToItems(builder);
    }

    protected boolean match(HasMetadata hasMetadata) {
        return match(hasMetadata, apiVersion, kind, name);
    }

    protected C config() {
        return config;
    }

    /**
     * Retrieves the name of the associated resource to be added
     *
     * @return the name of the resource to be added by this decorator
     */
    protected String name() {
        return name;
    }

    /**
     * Record interesting information or prepare the items list before resources are added.
     *
     * @param items the {@link HasMetadata} resources in the list (read-only)
     * @param list the associated builder lists (in case the resources in the list need to be modified)
     */
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
     */
    protected abstract void initBuilderWithDefaults(B builder);

    /**
     * Returns the first deployment-like resource with the given name if it exists in the specified list of items. Note
     * that we're using a method derived from dekorate but more optimized and avoiding rebuilding the list of items that we
     * already have available.
     *
     * @param items the list of items to search
     * @param name the name of the deployment-like resource to search for
     * @return the found deployment-like resource or {@link Optional#empty()}
     */
    protected static Optional<HasMetadata> deploymentNamed(List<HasMetadata> items, @Nullable String name) {
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

        return items.stream()
                .filter(h -> DEPLOYMENT_KINDS.contains(h.getKind()) &&
                        (Objects.equals(trimmed, ANY) || h.getMetadata().getName().equals(trimmed)))
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

    /**
     * Merges the provided labels with the ones provided by the designated deployment-like resource, identified by the optional
     * name. Note that when the same key is present in both label maps, the deployment-like resource's will take precedence.
     *
     * @param items the current list of generated resources
     * @param labels the labels to merge with the deployment-like resource's
     * @param deploymentName the name of the deployment-like resource
     * @return the merge labels map
     */
    protected Map<String, String> mergeLabelsFromDeploymentWith(List<HasMetadata> items, Map<String, String> labels,
            @Nullable String deploymentName) {
        Map<String, String> merged = new HashMap<>(labels);
        deploymentNamed(items, deploymentName)
                .map(hasMetadata -> hasMetadata.getMetadata().getLabels())
                .ifPresent(merged::putAll);
        return merged;
    }

    /**
     * Initializes the specified {@link LabelSelectorFluent} with a new empty, modifiable map
     *
     * @param selector the {@link LabelSelectorFluent} to modify
     * @return the modified selector fluent instance
     * @param <S> the specific type of the passed fluent instance
     */
    protected <S extends LabelSelectorFluent<?>> S initSelectorMatchLabels(S selector) {
        if (!selector.hasMatchLabels()) {
            selector.withMatchLabels(new HashMap<>());
        }
        return selector;
    }

    /**
     * Allows subclasses to add additional configuration to the main application container. Note that if you override this
     * method, you need to call this implementation first (via {@code super}) and you shouldn't call
     * {@link PodSpecFluent.ContainersNested#endContainer()} before returning the modified container, as it is further modified
     * at the call site.
     *
     * @param podSpec the spec where to look for the main application container
     * @param name the expected name of the container
     * @return a {@link PodSpecFluent.ContainersNested} allowing modification of the main application container
     * @param <PS> the type of the {@link PodSpecFluent}
     */
    protected <PS extends PodSpecFluent<?>> PodSpecFluent<?>.ContainersNested<?> createOrEditNamedContainer(PS podSpec,
            String name) {
        final var withName = (Predicate<ContainerBuilder>) cb -> cb.getName().equals(name);
        final PodSpecFluent<?>.ContainersNested<?> container;
        if (!podSpec.hasMatchingContainer(withName)) {
            container = podSpec.addNewContainer().withName(name);
        } else {
            container = podSpec.editMatchingContainer(withName);
        }
        return container;
    }
}
