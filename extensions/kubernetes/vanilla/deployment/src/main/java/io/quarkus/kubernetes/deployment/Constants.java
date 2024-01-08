package io.quarkus.kubernetes.deployment;

public final class Constants {

    public static final String KUBERNETES = "kubernetes";
    public static final String MINIKUBE = "minikube";
    public static final String KIND = "kind";
    public static final String STATEFULSET = "StatefulSet";
    public static final String DEPLOYMENT = "Deployment";
    public static final String JOB = "Job";
    public static final String CRONJOB = "CronJob";
    public static final String ROLE = "Role";
    public static final String CLUSTER_ROLE = "ClusterRole";
    public static final String ROLE_BINDING = "RoleBinding";
    public static final String CLUSTER_ROLE_BINDING = "ClusterRoleBinding";
    public static final String SERVICE_ACCOUNT = "ServiceAccount";
    public static final String DEPLOYMENT_GROUP = "apps";
    public static final String DEPLOYMENT_VERSION = "v1";
    public static final String INGRESS = "Ingress";
    public static final String BATCH_GROUP = "batch";
    public static final String BATCH_VERSION = "v1";
    public static final String JOB_API_VERSION = BATCH_GROUP + "/" + BATCH_VERSION;
    public static final String RBAC_API_GROUP = "rbac.authorization.k8s.io";
    public static final String RBAC_API_VERSION = RBAC_API_GROUP + "/v1";

    static final String DOCKER = "docker";

    public static final String OPENSHIFT = "openshift";
    public static final String DEPLOYMENT_CONFIG = "DeploymentConfig";
    public static final String DEPLOYMENT_CONFIG_GROUP = "apps.openshift.io";
    public static final String DEPLOYMENT_CONFIG_VERSION = "v1";

    public static final String ROUTE = "Route";
    public static final String ROUTE_API_GROUP = "route.openshift.io/v1";

    static final String OPENSHIFT_APP_RUNTIME = "app.openshift.io/runtime";
    static final String S2I = "s2i";
    static final String DEFAULT_S2I_IMAGE_NAME = "s2i-java"; //refers to the Dekorate default image.

    static final String OPENSHIFT_INTERNAL_REGISTRY = "image-registry.openshift-image-registry.svc:5000";
    static final String OPENSHIFT_INTERNAL_REGISTRY_PROJECT = "openshift-image-registry"; //a more relaxed str to match

    static final String KNATIVE = "knative";
    static final String KNATIVE_SERVICE = "Service";
    static final String KNATIVE_SERVICE_GROUP = "serving.knative.dev";
    static final String KNATIVE_SERVICE_VERSION = "v1";

    static final String OLD_DEPLOYMENT_TARGET = "kubernetes.deployment.target";
    static final String DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
    static final String DEPLOY = "quarkus.kubernetes.deploy";

    static final String QUARKUS = "quarkus";

    static final String QUARKUS_ANNOTATIONS_COMMIT_ID = "app.quarkus.io/commit-id";
    static final String QUARKUS_ANNOTATIONS_VCS_URL = "app.quarkus.io/vcs-uri";
    static final String QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP = "app.quarkus.io/build-timestamp";
    static final String QUARKUS_ANNOTATIONS_QUARKUS_VERSION = "app.quarkus.io/quarkus-version";

    public static final String HTTP_PORT = "http";
    public static final int DEFAULT_HTTP_PORT = 8080;

    public static final int MIN_PORT_NUMBER = 1;
    public static final int MAX_PORT_NUMBER = 65535;
    public static final int MIN_NODE_PORT_VALUE = 30000;
    public static final int MAX_NODE_PORT_VALUE = 31999;

    public static final String LIVENESS_PROBE = "livenessProbe";
    public static final String READINESS_PROBE = "readinessProbe";
    public static final String STARTUP_PROBE = "startupProbe";

    private Constants() {
    }
}
