
package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingRequirementBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

/***
 * Processor that handles the generation of ServiceBinding resources.
 * 
 * Generated ServiceBinding rules:
 *
 * 1. .metadata.name:
 * When no user configuration is present then the combination [app.name] - [qualifier kind] + [qualifier name] will be used.
 *
 * Examples:
 * - app-postgresql-default
 * - app-mysql-default
 * - app-mongodb-default
 * 
 * 2. .spec.service[*].name:
 * Since services are qualified using apiVersion, we don't need to carry over the [qualifier kind]. In this case the name is
 * just [qualifier name].*
 * 
 */
public class ServiceBindingProcessor {

    protected static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("postgresql", "PostgresCluster.postgres-operator.crunchydata.com/v1beta1");
        DEFAULTS.put("mysql", "PerconaXtraDBCluster.pxc.percona.com/v1-9-0");
        DEFAULTS.put("redis", "Redis.redis.redis.opstreelabs.in/v1beta1");
        DEFAULTS.put("mongodb", "PerconaServerMongoDB.psmdb.percona.com/v1-9-0");
        DEFAULTS.put("kafka", "Kafka.kafka.strimzi.io/v1beta2");
    }

    @BuildStep
    public List<ServiceBindingRequirementBuildItem> createServiceBindingDecorators(ApplicationInfoBuildItem applicationInfo,
            KubernetesServiceBindingConfig config, List<ServiceBindingQualifierBuildItem> qualifiers) {
        Map<String, ServiceBindingRequirementBuildItem> requirements = new HashMap<>();

        String applicationName = applicationInfo.getName();
        //First we add all user provided services
        config.services.forEach((key, s) -> {
            createRequirement(applicationName, key, config).ifPresent(r -> requirements.put(key, r));
        });

        //Then we try to make requirements out of qualifies for services not already provided by the user
        qualifiers.forEach(q -> {
            Optional<ServiceBindingRequirementBuildItem> requirement = createRequirement(applicationName, config, q);
            requirement.ifPresent(r -> {
                String id = q.getId();
                requirements.putIfAbsent(id, r);
            });
        });
        return requirements.values().stream().collect(Collectors.toList());
    }

    @BuildStep
    public List<DecoratorBuildItem> createServiceBindingDecorators(KubernetesServiceBindingConfig serviceBindingConfig,
            List<ServiceBindingRequirementBuildItem> services, List<KubernetesResourceMetadataBuildItem> resources) {
        List<DecoratorBuildItem> result = new ArrayList<>();
        resources.stream()
                .distinct()
                .forEach(r -> {
                    if (!services.isEmpty()) {
                        services.forEach(service -> {
                            result.add(new DecoratorBuildItem(r.getTarget(), new AddServiceBindingResourceDecorator(
                                    r.getGroup(), r.getVersion(), r.getKind(), r.getName(), serviceBindingConfig, service)));
                        });
                    }
                });
        return result;
    }

    protected static Optional<ServiceBindingRequirementBuildItem> createRequirement(String applicationName, String serviceKey,
            KubernetesServiceBindingConfig config) {
        String name = config.services != null && config.services.containsKey(serviceKey)
                ? config.services.get(serviceKey).name.orElse(serviceKey)
                : serviceKey;

        return createRequirement(applicationName, serviceKey, serviceKey, name, config);
    }

    protected static Optional<ServiceBindingRequirementBuildItem> createRequirement(String applicationName, String serviceKey,
            String serviceId, String serviceName, KubernetesServiceBindingConfig config) {
        if (config.services != null && config.services.containsKey(serviceId)) {
            ServiceConfig provided = config.services.get(serviceId);
            String apiVersion = provided.apiVersion
                    .orElseGet(() -> getDefaultQualifiedKind(serviceKey).map(ServiceBindingProcessor::apiVersion).orElse(""));
            String kind = provided.kind.orElseGet(() -> getDefaultQualifiedKind(serviceKey).map(ServiceBindingProcessor::kind)
                    .orElseThrow(() -> new IllegalStateException("Failed to determing bindable service kind.")));
            //When a service is partically or fully configured, use the configured binding or fallback to the application name and service id combination.
            return Optional
                    .of(new ServiceBindingRequirementBuildItem(provided.binding.orElse(applicationName + "-" + serviceId),
                            apiVersion, kind, provided.name.orElse(serviceName)));
        }
        return Optional.empty();
    }

    protected static Optional<ServiceBindingRequirementBuildItem> createRequirement(String applicationName,
            KubernetesServiceBindingConfig config, ServiceBindingQualifierBuildItem qualifier) {
        String serviceId = qualifier.getId();

        if (config.services != null && config.services.containsKey(serviceId)) {
            return createRequirement(applicationName, qualifier.getKind(), serviceId, qualifier.getName(), config);
        } else if (DEFAULTS.containsKey(qualifier.getKind())) {
            String value = DEFAULTS.get(qualifier.getKind());
            //When no service is configured, we use as binding name the combination of kind and name.
            return Optional.of(new ServiceBindingRequirementBuildItem(applicationName + "-" + serviceId, apiVersion(value),
                    kind(value), serviceId));
        }
        return Optional.empty();
    }

    protected static Optional<String> getDefaultQualifiedKind(String id) {
        if (!DEFAULTS.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(DEFAULTS.get(id));
    }

    protected static String apiVersion(String name) {
        return name.substring(name.indexOf(".") + 1);
    }

    protected static String kind(String name) {
        return name.substring(0, name.indexOf("."));
    }
}
