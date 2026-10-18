package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.BATCH_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.BATCH_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.CRONJOB;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.JOB;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE_SERVICE;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE_SERVICE_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE_SERVICE_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.STATEFULSET;

import java.util.Set;

import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.HasMetadata;

public enum DeploymentResourceKind {

    Deployment(DEPLOYMENT, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION),
    @Deprecated(since = "OpenShift 4.14")
    DeploymentConfig(DEPLOYMENT_CONFIG, DEPLOYMENT_CONFIG_GROUP, DEPLOYMENT_CONFIG_VERSION, OPENSHIFT),
    StatefulSet(STATEFULSET, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION),
    Job(JOB, BATCH_GROUP, BATCH_VERSION),
    CronJob(CRONJOB, BATCH_GROUP, BATCH_VERSION),
    KnativeService(KNATIVE_SERVICE, KNATIVE_SERVICE_GROUP, KNATIVE_SERVICE_VERSION, KNATIVE);

    private final String kind;
    private final String group;
    private final String version;
    private final Set<String> requiredTargets;

    DeploymentResourceKind(String kind, String group, String version, String... requiredTargets) {
        this(kind, group, version, Set.of(requiredTargets));
    }

    DeploymentResourceKind(String kind, String group, String version, Set<String> requiredTargets) {
        this.kind = kind;
        this.group = group;
        this.version = version;
        this.requiredTargets = requiredTargets;
    }

    public static DeploymentResourceKind find(String apiGroup, String apiVersion, String kind) {
        for (DeploymentResourceKind deploymentResourceKind : DeploymentResourceKind.values()) {
            if (deploymentResourceKind.kind.equals(kind) && deploymentResourceKind.group.equals(apiGroup)
                    && deploymentResourceKind.version.equals(apiVersion)) {
                return deploymentResourceKind;
            }
        }
        String apiGroupVersion = Strings.isNullOrEmpty(apiGroup) ? apiVersion : apiGroup + "/" + apiVersion;
        throw new IllegalArgumentException("Could not find DeploymentResourceKind for " + apiGroupVersion + " " + kind);
    }

    public boolean isAvailalbleOn(String target) {
        return requiredTargets.isEmpty() || requiredTargets.contains(target);
    }

    public boolean matches(HasMetadata resource) {
        String resourceKind = HasMetadata.getKind(resource.getClass());
        String resourceVersion = HasMetadata.getApiVersion(resource.getClass());
        return resourceKind.equals(getKind()) && resourceVersion.equals(getApiVersion());
    }

    public String getKind() {
        return kind;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public Set<String> getRequiredTargets() {
        return requiredTargets;
    }

    public String getApiVersion() {
        return group + "/" + version;
    }
}
