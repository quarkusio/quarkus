package io.quarkus.kubernetes.client.deployment;

import java.util.function.BooleanSupplier;

/**
 * Boolean supplier that returns true if quarkus-test-kubernetes-client is not
 * present in the application pom.xml.
 * <p>
 * quarkus-test-kubernetes-client provide a Kubernetes client configuration to connect
 * to a Kubernetes mock server and will have precedence over the configuration provided by
 * Dev Services for Kubernetes. DevServicesKubernetesProcessor uses this BooleanSupplier
 * to avoid starting a Kubernetes test container in such a case.
 */
class NoQuarkusTestKubernetesClient implements BooleanSupplier {
    static final String IO_QUARKUS_TEST_KUBERNETES_CLIENT_PACKAGE_CLASS = "io.quarkus.test.kubernetes.client.AbstractKubernetesTestResource";
    // We cannot assume what order classes are loaded in, so check if a known class can be loaded rather than looking at what's already loaded
    static final boolean IO_QUARKUS_TEST_KUBERNETES_CLIENT_AVAILABLE = isClassAvailable(
            IO_QUARKUS_TEST_KUBERNETES_CLIENT_PACKAGE_CLASS);

    @Override
    public boolean getAsBoolean() {
        return !IO_QUARKUS_TEST_KUBERNETES_CLIENT_AVAILABLE;
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
