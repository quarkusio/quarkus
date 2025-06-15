package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE;
import static io.quarkus.kubernetes.deployment.Constants.CLUSTER_ROLE_BINDING;
import static io.quarkus.kubernetes.deployment.Constants.CRONJOB;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG;
import static io.quarkus.kubernetes.deployment.Constants.INGRESS;
import static io.quarkus.kubernetes.deployment.Constants.JOB;
import static io.quarkus.kubernetes.deployment.Constants.ROLE;
import static io.quarkus.kubernetes.deployment.Constants.ROLE_BINDING;
import static io.quarkus.kubernetes.deployment.Constants.ROUTE;
import static io.quarkus.kubernetes.deployment.Constants.SERVICE_ACCOUNT;
import static io.quarkus.kubernetes.deployment.Constants.STATEFULSET;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.dekorate.processor.SimpleFileWriter;
import io.dekorate.project.Project;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class QuarkusFileWriter extends SimpleFileWriter {

    private static final List<String> RESOURCE_KIND_ORDER = List.of("Namespace", "NetworkPolicy", "ResourceQuota",
            "LimitRange", "PodSecurityPolicy", "PodDisruptionBudget", SERVICE_ACCOUNT, "Secret", "SecretList",
            "ConfigMap", "StorageClass", "PersistentVolume", "PersistentVolumeClaim", "CustomResourceDefinition",
            CLUSTER_ROLE, "ClusterRoleList", CLUSTER_ROLE_BINDING, "ClusterRoleBindingList", ROLE, "RoleList",
            ROLE_BINDING, "RoleBindingList", "Service", "ImageStream", "BuildConfig", "DaemonSet", "Pod",
            "ReplicationController", "ReplicaSet", DEPLOYMENT, "HorizontalPodAutoscaler", STATEFULSET,
            DEPLOYMENT_CONFIG, JOB, CRONJOB, INGRESS, ROUTE, "APIService");

    public QuarkusFileWriter(Project project) {
        super(project, false);
    }

    @Override
    public Map<String, String> write(String group, KubernetesList list) {
        // sort resources in list by: ServiceAccount, Role, ClusterRole, the rest...
        return super.write(group, new KubernetesListBuilder().addAllToItems(sort(list.getItems())).build());
    }

    private List<HasMetadata> sort(List<HasMetadata> items) {
        // Resources that we need the order.
        Map<String, List<HasMetadata>> groups = new HashMap<>();
        // List of resources with unknown order: we preserve the order of creation in this case
        List<HasMetadata> rest = new LinkedList<>();
        for (HasMetadata item : items) {
            String kind = item.getKind();
            if (RESOURCE_KIND_ORDER.contains(kind)) {
                groups.computeIfAbsent(kind, k -> new LinkedList<>()).add(item);
            } else {
                rest.add(item);
            }
        }

        List<HasMetadata> sorted = new LinkedList<>();
        // we first add the resources with order
        for (String kind : RESOURCE_KIND_ORDER) {
            List<HasMetadata> group = groups.get(kind);
            if (group != null) {
                sorted.addAll(group);
            }
        }

        sorted.addAll(rest);
        return sorted;
    }
}
