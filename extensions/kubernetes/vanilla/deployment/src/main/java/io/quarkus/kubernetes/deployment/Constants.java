package io.quarkus.kubernetes.deployment;

public class Constants {

    static final String KUBERNETES = "kubernetes";
    static final String DEPLOYMENT = "Deployment";
    static final String DOCKER = "docker";

    static final String OPENSHIFT = "openshift";
    static final String OPENSHIFT_APP_RUNTIME = "app.openshift.io/runtime";
    static final String DEPLOYMENT_CONFIG = "DeploymentConfig";
    static final String S2I = "s2i";
    static final String DEFAULT_S2I_IMAGE_NAME = "s2i-java";

    static final String KNATIVE = "knative";
    static final String SERVICE = "Service";

    static final String OLD_DEPLOYMENT_TARGET = "kubernetes.deployment.target";
    static final String DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
    static final String DEPLOY = "quarkus.kubernetes.deploy";

    static final String QUARKUS = "quarkus";

    static final String QUARKUS_ANNOTATIONS_COMMIT_ID = "app.quarkus.io/commit-id";
    static final String QUARKUS_ANNOTATIONS_VCS_URL = "app.quarkus.io/vcs-url";
    static final String QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP = "app.quarkus.io/build-timestamp";

    static final String OPENSHIFT_ANNOTATIONS_VCS_URL = "app.openshift.io/vcs-url";
}
