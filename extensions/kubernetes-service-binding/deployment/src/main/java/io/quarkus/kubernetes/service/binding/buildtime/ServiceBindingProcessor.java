
package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.service.binding.spi.ServiceQualifierBuildItem;
import io.quarkus.kubernetes.service.binding.spi.ServiceRequirementBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

public class ServiceBindingProcessor {

    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("postgresql", "PostgresCluster.postgres-operator.crunchydata.com/v1beta1");
        DEFAULTS.put("mysql", "PerconaXtraDBCluster.pxc.percona.com/v1-9-0");
        DEFAULTS.put("redis", "Redis.redis.redis.opstreelabs.in/v1beta1");
        DEFAULTS.put("mongo", "PerconaServerMongoDB.psmdb.percona.com/v1-9-0");
        DEFAULTS.put("kafka", "Kafka.kafka.strimzi.io/v1beta2");
    }

    @BuildStep
    public List<ServiceRequirementBuildItem> createServiceBindingDecorators(KubernetesServiceBindingConfig config,
            List<ServiceQualifierBuildItem> qualifiers) {
        Map<String, ServiceRequirementBuildItem> requirements = new HashMap<>();

        //First we add all user provided services
        config.services.forEach((id, s) -> {
            requirements.put(id, new ServiceRequirementBuildItem(s.apiVersion, s.kind, s.name.orElse(id)));
        });

        //Then we try to make requirements out of qualifies for services not already provided by the user
        qualifiers.forEach(q -> {
            Optional<ServiceRequirementBuildItem> requirement = createRequirement(config, q);
            requirement.ifPresent(r -> {
                requirements.putIfAbsent(r.getName(), r);
            });
        });
        return requirements.values().stream().collect(Collectors.toList());
    }

    @BuildStep
    public List<DecoratorBuildItem> createServiceBindingDecorators(KubernetesServiceBindingConfig serviceBindingConfig,
            List<ServiceRequirementBuildItem> services, List<KubernetesResourceMetadataBuildItem> resources) {
        List<DecoratorBuildItem> result = new ArrayList<>();
        resources.stream()
                .distinct()
                .forEach(r -> {
                    if (!services.isEmpty()) {
                        result.add(new DecoratorBuildItem(r.getTarget(), new AddServiceBindingResourceDecorator(r.getGroup(),
                                r.getVersion(), r.getKind(), r.getName(), serviceBindingConfig, services)));
                    }
                });
        return result;
    }

    private static Optional<ServiceRequirementBuildItem> createRequirement(KubernetesServiceBindingConfig config,
            ServiceQualifierBuildItem qualifier) {
        String id = qualifier.getName();
        if (config.services.containsKey(id)) {
            ServiceConfig provided = config.services.get(id);
            return Optional.of(new ServiceRequirementBuildItem(provided.apiVersion, provided.kind, provided.name.orElse(id)));
        } else if (DEFAULTS.containsKey(id)) {
            String qualifiedName = DEFAULTS.get(id);
            String kind = qualifiedName.substring(0, qualifiedName.indexOf("."));
            String apiVersion = qualifiedName.substring(qualifiedName.indexOf(".") + 1);
            return Optional.of(new ServiceRequirementBuildItem(apiVersion, kind, id));
        }
        return Optional.empty();
    }
}
