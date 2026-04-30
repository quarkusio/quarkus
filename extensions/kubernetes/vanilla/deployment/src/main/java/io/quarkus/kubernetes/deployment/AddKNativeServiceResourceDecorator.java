package io.quarkus.kubernetes.deployment;

import java.util.List;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class AddKNativeServiceResourceDecorator
        extends BaseAddDeploymentResourceDecorator<Service, ServiceBuilder, KnativeConfig> {
    private static final String MIN_SCALE = "autoscaling.knative.dev/min-scale";
    private static final String MAX_SCALE = "autoscaling.knative.dev/max-scale";
    private static final String AUTOSCALING_CLASS = "autoscaling.knative.dev/class";
    private static final String AUTOSCALING_CLASS_SUFFIX = ".autoscaling.knative.dev";
    private static final String AUTOSCALING_METRIC = "autoscaling.knative.dev/metric";
    private static final String UTILIZATION_PERCENTAGE = "autoscaling.knative.dev/target-utilization-percentage";
    private static final String AUTOSCALING_TARGET = "autoscaling.knative.dev/target";
    private static final String LATEST_REVISION = "latest";

    public AddKNativeServiceResourceDecorator(String name, KnativeConfig config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.KnativeService, config, toRemove);
    }

    @Override
    protected ServiceBuilder builderWithName(String name) {
        return new ServiceBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void prepare(List<HasMetadata> items, KubernetesListBuilder list) {
        super.prepare(items, list);

        final var config = config();
        final var globalAutoScaling = config.globalAutoScaling();

        // create autoscaler ConfigMap if needed
        if (config.needAutoScalerConfigMap()) {
            new AutoScalerConfigMap(config).addOrEditExisting(items, list);
        }

        // create defaults ConfigMap if needed
        globalAutoScaling.containerConcurrency().ifPresent(c -> new DefaultsConfigMap(config).addOrEditExisting(items, list));
    }

    // Reusing the generic decorator behavior to add/edit KNative ConfigMaps if needed
    private abstract static class KNativeConfigMap
            extends BaseAddResourceDecorator<ConfigMap, ConfigMapBuilder, KnativeConfig> {
        public KNativeConfigMap(String name, KnativeConfig config) {
            super(name, ConfigMap.class, config);
        }

        @Override
        protected ConfigMapBuilder builderWithName(String name) {
            return new ConfigMapBuilder().withNewMetadata()
                    .withName(name)
                    .withNamespace("knative-serving")
                    .endMetadata();
        }
    }

    // Takes care of creating (or editing, if already present on the cluster) the autoscaler ConfigMap
    private static class AutoScalerConfigMap extends KNativeConfigMap {
        public AutoScalerConfigMap(KnativeConfig config) {
            super("config-autoscaler", config);
        }

        @Override
        protected void initBuilderWithDefaults(ConfigMapBuilder builder) {
            final var config = config();
            final var globalAutoScaling = config.globalAutoScaling();

            globalAutoScaling.autoScalerClass()
                    .ifPresent(
                            a -> builder.addToData("pod-autoscaler-class", a.name().toLowerCase() + AUTOSCALING_CLASS_SUFFIX));

            globalAutoScaling.requestsPerSecond()
                    .ifPresent(r -> builder.addToData("requests-per-second-target-default", String.valueOf(r)));

            globalAutoScaling.targetUtilizationPercentage()
                    .ifPresent(t -> builder.addToData("container-concurrency-target-default", String.valueOf(t)));

            if (!config.scaleToZeroEnabled()) {
                builder.addToData("enable-scale-to-zero", String.valueOf(false));
            }
        }
    }

    // Takes care of creating (or editing, if already present on the cluster) the defaults ConfigMap
    private static class DefaultsConfigMap extends KNativeConfigMap {
        public DefaultsConfigMap(KnativeConfig config) {
            super("config-defaults", config);
        }

        @Override
        protected void initBuilderWithDefaults(ConfigMapBuilder builder) {
            config().globalAutoScaling().containerConcurrency()
                    .ifPresent(c -> builder.addToData("container-concurrency", String.valueOf(c)));
        }
    }

    @Override
    protected void initBuilderWithDefaults(ServiceBuilder builder) {
        final var config = config();

        final var spec = builder.editOrNewSpec();

        // traffic config
        config.traffic().forEach((k, traffic) -> {
            //Revision name is K unless we have the edge name of a revision named 'latest' which is not really the latest (in which case use null).
            boolean latestRevision = traffic.latestRevision().get();
            String revisionName = !latestRevision && LATEST_REVISION.equals(k) ? null : k;
            String tag = traffic.tag().orElse(null);
            long percent = traffic.percent().orElse(100L);
            spec.addNewTraffic().withRevisionName(revisionName)
                    .withLatestRevision(latestRevision)
                    .withPercent(percent)
                    .withTag(tag)
                    .endTraffic();

        });

        final var revisionTemplate = spec.editOrNewTemplate();

        final var templateMetadata = revisionTemplate.editOrNewMetadata();
        config.minScale().ifPresent(min -> templateMetadata.addToAnnotations(MIN_SCALE, String.valueOf(min)));
        config.maxScale().ifPresent(max -> templateMetadata.addToAnnotations(MAX_SCALE, String.valueOf(max)));
        final var revisionAutoScaling = config.revisionAutoScaling();
        revisionAutoScaling.autoScalerClass()
                .ifPresent(a -> templateMetadata.addToAnnotations(AUTOSCALING_CLASS,
                        a.name().toLowerCase() + AUTOSCALING_CLASS_SUFFIX));
        revisionAutoScaling.metric()
                .ifPresent(m -> templateMetadata.addToAnnotations(AUTOSCALING_METRIC, m.name().toLowerCase()));
        revisionAutoScaling.targetUtilizationPercentage()
                .ifPresent(t -> templateMetadata.addToAnnotations(UTILIZATION_PERCENTAGE, String.valueOf(t)));
        revisionAutoScaling.target()
                .ifPresent(t -> templateMetadata.addToAnnotations(AUTOSCALING_TARGET, String.valueOf(t)));

        //Traffic Splitting
        config.revisionName().ifPresent(templateMetadata::withName);

        templateMetadata.endMetadata();

        final var revisionSpec = revisionTemplate.editOrNewSpec();
        revisionAutoScaling.containerConcurrency()
                .map(Integer::longValue)
                .ifPresent(revisionSpec::withContainerConcurrency);

        revisionSpec.addAllToVolumes(configureVolumes());
        revisionSpec.endSpec();

        revisionTemplate.endTemplate();
        spec.endSpec();
    }
}
