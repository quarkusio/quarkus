package io.quarkus.kubernetes.deployment;

public final class Constants {

    static final String KUBERNETES = "kubernetes";
    public static final String MINIKUBE = "minikube";
    public static final String DEPLOYMENT = "Deployment";
    static final String DOCKER = "docker";

    public static final String OPENSHIFT = "openshift";
    public static final String DEPLOYMENT_CONFIG = "DeploymentConfig";
    static final String OPENSHIFT_APP_RUNTIME = "app.openshift.io/runtime";
    static final String S2I = "s2i";
    static final String DEFAULT_S2I_IMAGE_NAME = "s2i-java"; //refers to the Dekorate default image.

    static final String KNATIVE = "knative";
    static final String SERVICE = "Service";

    static final String OLD_DEPLOYMENT_TARGET = "kubernetes.deployment.target";
    static final String DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
    static final String DEPLOY = "quarkus.kubernetes.deploy";

    static final String QUARKUS = "quarkus";

    static final String QUARKUS_ANNOTATIONS_COMMIT_ID = "app.quarkus.io/commit-id";
    static final String QUARKUS_ANNOTATIONS_VCS_URL = "app.quarkus.io/vcs-url";
    static final String QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP = "app.quarkus.io/build-timestamp";

    static final String HTTP_PORT = "http";
    static final int DEFAULT_HTTP_PORT = 8080;

    static final int MIN_PORT_NUMBER = 1;
    static final int MAX_PORT_NUMBER = 65535;
    static final int MIN_NODE_PORT_VALUE = 30000;
    static final int MAX_NODE_PORT_VALUE = 31999;

    private Constants() {
    }
}
