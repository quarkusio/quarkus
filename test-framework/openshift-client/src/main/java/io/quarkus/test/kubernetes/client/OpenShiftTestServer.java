package io.quarkus.test.kubernetes.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.fabric8.openshift.client.server.mock.OpenShiftServer;

/**
 * Used to specify that the field should be injected with the mock OpenShift API server
 * Can only be used on type {@link OpenShiftServer}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OpenShiftTestServer {
}
