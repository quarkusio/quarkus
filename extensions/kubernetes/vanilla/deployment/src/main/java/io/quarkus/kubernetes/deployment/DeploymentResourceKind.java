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

public enum DeploymentResourceKind {

    Deployment(DEPLOYMENT, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION),
    @Deprecated(since = "OpenShift 4.14")
    DeploymentConfig(DEPLOYMENT_CONFIG, DEPLOYMENT_CONFIG_GROUP, DEPLOYMENT_CONFIG_VERSION, OPENSHIFT),
    StatefulSet(STATEFULSET, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION),
    Job(JOB, BATCH_GROUP, BATCH_VERSION),
    CronJob(CRONJOB, BATCH_GROUP, BATCH_VERSION),
    KnativeService(KNATIVE_SERVICE, KNATIVE_SERVICE_GROUP, KNATIVE_SERVICE_VERSION, KNATIVE);

    public final String kind;
    public final String apiGroup;
    public final String apiVersion;
    public final Set<String> requiredTargets;

    DeploymentResourceKind(String kind, String apiGroup, String apiVersion, String... requiredTargets) {
        this(kind, apiGroup, apiVersion, Set.of(requiredTargets));
    }

    DeploymentResourceKind(String kind, String apiGroup, String apiVersion, Set<String> requiredTargets) {
        this.kind = kind;
        this.apiGroup = apiGroup;
        this.apiVersion = apiVersion;
        this.requiredTargets = requiredTargets;
    }

    public boolean isAvailalbleOn(String target) {
        return requiredTargets.isEmpty() || requiredTargets.contains(target);
    }
}
