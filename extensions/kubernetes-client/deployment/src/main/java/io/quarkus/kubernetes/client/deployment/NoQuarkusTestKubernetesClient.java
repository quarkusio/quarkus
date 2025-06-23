package io.quarkus.kubernetes.client.deployment;

import java.util.Arrays;
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
    static final String IO_QUARKUS_TEST_KUBERNETES_CLIENT_PACKAGE = "io.quarkus.test.kubernetes.client";
    static final Boolean IO_QUARKUS_TEST_KUBERNETES_CLIENT_AVAILABLE = Arrays.stream(Package.getPackages())
            .map(Package::getName)
            .anyMatch(p -> p.startsWith(IO_QUARKUS_TEST_KUBERNETES_CLIENT_PACKAGE));

    @Override
    public boolean getAsBoolean() {
        return !IO_QUARKUS_TEST_KUBERNETES_CLIENT_AVAILABLE;
    }
}
