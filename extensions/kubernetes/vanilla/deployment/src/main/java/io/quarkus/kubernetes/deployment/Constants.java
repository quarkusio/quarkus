package io.quarkus.kubernetes.deployment;

public final class Constants {

    public static final String KUBERNETES = "kubernetes";
    public static final String MINIKUBE = "minikube";
    public static final String DEPLOYMENT = "Deployment";
    public static final String DEPLOYMENT_GROUP = "apps";
    public static final String DEPLOYMENT_VERSION = "v1";
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

    static final String KNATIVE = "knative";
    static final String KNATIVE_SERVICE = "Service";
    static final String KNATIVE_SERVICE_GROUP = "serving.knative.dev";
    static final String KNATIVE_SERVICE_VERSION = "v1";

    static final String OLD_DEPLOYMENT_TARGET = "kubernetes.deployment.target";
    static final String DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
    static final String DEPLOY = "quarkus.kubernetes.deploy";

    static final String QUARKUS = "quarkus";

    static final String QUARKUS_ANNOTATIONS_COMMIT_ID = "app.quarkus.io/commit-id";
    static final String QUARKUS_ANNOTATIONS_VCS_URL = "app.quarkus.io/vcs-url";
    static final String QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP = "app.quarkus.io/build-timestamp";

    public static final String HTTP_PORT = "http";
    public static final int DEFAULT_HTTP_PORT = 8080;

    public static final int MIN_PORT_NUMBER = 1;
    public static final int MAX_PORT_NUMBER = 65535;
    public static final int MIN_NODE_PORT_VALUE = 30000;
    public static final int MAX_NODE_PORT_VALUE = 31999;

    private Constants() {
    }
}
