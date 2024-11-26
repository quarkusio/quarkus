
package io.quarkus.kubernetes.service.binding.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingRequirementBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesOptionalResourceDefinitionBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

/***
 * Processor that handles the generation of ServiceBinding resources.
 *
 * Generated ServiceBinding rules:
 *
 * 1. .metadata.name:
 * When no user configuration is present then the combination [app.name] - [qualifier kind] - [qualifier name] will be used.
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
 * Notes:
 *
 * The following pairs are equivalent:
 *
 * quarkus.kubernetes-service-binding.services.postgresql-default.xxx=yyy
 * quarkus.kubernetes-service-binding.services.postgresql.xxx=yyy
 *
 * quarkus.kubernetes-service-binding.services.postgresql-persondb.xxx=yyy
 * quarkus.kubernetes-service-binding.services.persondb.xxx=yyy
 *
 * When service are auto bound the minimal from (e.g. persondb) will be used.
 * Users are still able to tune things using the [kind]-[name] combo to avoid naming clashes.
 *
 */
public class ServiceBindingProcessor {

    protected static final Map<String, String> DEFAULTS = new HashMap<>();

    private static final String KIND = "ServiceBinding";
    private static final String API_VERSION = "binding.operators.coreos.com/v1alpha1";

    static {
        DEFAULTS.put("postgresql", "PostgresCluster.postgres-operator.crunchydata.com/v1beta1");
        DEFAULTS.put("mysql", "PerconaXtraDBCluster.pxc.percona.com/v1-9-0");
        DEFAULTS.put("redis", "Redis.redis.redis.opstreelabs.in/v1beta1");
        DEFAULTS.put("mongodb", "PerconaServerMongoDB.psmdb.percona.com/v1-9-0");
        DEFAULTS.put("kafka", "Kafka.kafka.strimzi.io/v1beta2");
    }

    @BuildStep
    public void registerServiceBindingAsOptional(BuildProducer<KubernetesOptionalResourceDefinitionBuildItem> optionalKinds) {
        optionalKinds.produce(new KubernetesOptionalResourceDefinitionBuildItem(API_VERSION, KIND));
    }

    @BuildStep
    public List<ServiceBindingRequirementBuildItem> createServiceBindingDecorators(ApplicationInfoBuildItem applicationInfo,
            KubernetesServiceBindingConfig config, List<ServiceBindingQualifierBuildItem> qualifiers) {
        Map<String, ServiceBindingRequirementBuildItem> requirements = new HashMap<>();

        String applicationName = applicationInfo.getName();
        //First we add all user provided services
        config.services().forEach((key, s) -> {
            createRequirementFromConfig(applicationName, key, config).ifPresent(r -> requirements.put(key, r));
        });

        //Then we try to make requirements out of qualifies for services not already provided by the user
        qualifiers.forEach(q -> {
            Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier(applicationName, config,
                    q);
            requirement.ifPresent(r -> {
                String id = q.getId();
                requirements.putIfAbsent(id, r);
            });
        });
        return new ArrayList<>(requirements.values());
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

    /**
     * Create an {@link Optional} {@link ServiceBindingRequirementBuildItem} from a {@link KubernetesServiceBindingConfig}
     * entry.
     *
     * @param applicationName The name of the application.
     * @param serviceId The key of the service.
     * @param config The config instance.
     * @return The service binding requirement that corresponds to the matching entry, or empty if no service with serviceId was
     *         found in config.
     */
    protected static Optional<ServiceBindingRequirementBuildItem> createRequirementFromConfig(String applicationName,
            String serviceId, KubernetesServiceBindingConfig config) {
        String name = config.services() != null && config.services().containsKey(serviceId)
                ? config.services().get(serviceId).name().orElse(serviceId)
                : serviceId;

        return createRequirementFromConfig(applicationName, serviceId, serviceId, name, config);
    }

    /**
     * Create an {@link Optional} {@link ServiceBindingRequirementBuildItem} from a {@link KubernetesServiceBindingConfig}
     * entry.
     *
     * @param applicationName The name of the application.
     * @param defaultsLookupKey The key to use for looking up default apiVersion/kind).
     * @param serviceId The key of the service.
     * @param resourceName The name of the target resource.
     * @param config The config instance.
     * @return The service binding requirement that corresponds to the matching entry, or empty if no service with serviceId was
     *         found in config.
     */
    protected static Optional<ServiceBindingRequirementBuildItem> createRequirementFromConfig(String applicationName,
            String defaultsLookupKey, String serviceId, String resourceName, KubernetesServiceBindingConfig config) {
        if (config.services() != null && config.services().containsKey(serviceId)) {
            ServiceConfig provided = config.services().get(serviceId);
            String apiVersion = provided.apiVersion().orElseGet(
                    () -> getDefaultQualifiedKind(defaultsLookupKey).map(ServiceBindingProcessor::apiVersion).orElse(""));
            String kind = provided.kind()
                    .orElseGet(() -> getDefaultQualifiedKind(defaultsLookupKey).map(ServiceBindingProcessor::kind)
                            .orElseThrow(() -> new IllegalStateException("Failed to determine bindable service kind.")));
            //When a service is partially or fully configured, use the configured binding or fallback to the application name and service id combination.
            return Optional
                    .of(new ServiceBindingRequirementBuildItem(provided.binding().orElse(applicationName + "-" + serviceId),
                            apiVersion, kind, provided.name().orElse(resourceName)));
        }
        return Optional.empty();
    }

    /**
     * Create an {@link Optional} {@link ServiceBindingRequirementBuildItem} from a {@link ServiceBindingQualifierBuildItem}.
     *
     * @param applicationName The name of the application.
     * @param config The config instance.
     * @param qualifier The qualifier that will be converted to a requirement.
     * @return The service binding requirement that corresponds to the config entry that matches the qualifier, or the defaults
     *         for the qualifier. Returns empty if none of the above was found.
     */
    protected static Optional<ServiceBindingRequirementBuildItem> createRequirementFromQualifier(String applicationName,
            KubernetesServiceBindingConfig config, ServiceBindingQualifierBuildItem qualifier) {

        if (config.services() != null && config.services().containsKey(qualifier.getId())) {
            return createRequirementFromConfig(applicationName, qualifier.getKind(), qualifier.getId(), qualifier.getName(),
                    config);
        } else if (config.services() != null && config.services().containsKey(qualifier.getName())) {
            return createRequirementFromConfig(applicationName, qualifier.getKind(), qualifier.getName(), qualifier.getName(),
                    config);
        } else if (DEFAULTS.containsKey(qualifier.getKind())) {
            String value = DEFAULTS.get(qualifier.getKind());
            //When no service is configured, we use as binding name the combination of kind and name.
            return Optional.of(new ServiceBindingRequirementBuildItem(applicationName + "-" + qualifier.getId(),
                    apiVersion(value), kind(value), qualifier.getId()));
        }
        return Optional.empty();
    }

    protected static Optional<String> getDefaultQualifiedKind(String key) {
        if (!DEFAULTS.containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(DEFAULTS.get(key));
    }

    protected static String apiVersion(String name) {
        return name.substring(name.indexOf(".") + 1);
    }

    protected static String kind(String name) {
        return name.substring(0, name.indexOf("."));
    }
}
