package io.quarkus.kubernetes.deployment;

public class Constants {

    static final String KUBERNETES = "kubernetes";
    static final String DEPLOYMENT = "Deployment";
    static final String DOCKER = "docker";

    static final String OPENSHIFT = "openshift";
    static final String DEPLOYMENT_CONFIG = "DeploymentConfig";
    static final String S2I = "s2i";

    static final String KNATIVE = "knative";
    static final String SERVICE = "Service";

    static final String OLD_DEPLOYMENT_TARGET = "kubernetes.deployment.target";
    static final String DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
    static final String DEPLOY = "quarkus.kubernetes.deploy";
}
