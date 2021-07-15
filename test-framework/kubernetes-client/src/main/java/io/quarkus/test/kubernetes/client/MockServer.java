package io.quarkus.test.kubernetes.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;

/**
 * Used to specify that the field should be injected with the mock Kubernetes API server
 * Can only be used on type {@link KubernetesMockServer}
 *
 * @deprecated use {@link KubernetesTestServer}
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MockServer {
}
