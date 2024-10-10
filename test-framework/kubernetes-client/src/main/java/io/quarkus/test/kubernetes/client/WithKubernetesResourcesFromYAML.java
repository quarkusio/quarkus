package io.quarkus.test.kubernetes.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(value = YAMLKubernetesFixturesTestResource.class, restrictToAnnotatedClass = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithKubernetesResourcesFromYAML {
    String[] yamlFiles() default {};

    String namespace() default AnnotationConstants.UNSET_STRING_VALUE;

    boolean createNamespaceIfNeeded() default true;

    boolean preserveNamespaceOnError() default false;

    int secondsToWaitForNamespaceDeletion() default 0;

    int readinessTimeoutSeconds() default AnnotationConstants.DEFAULT_READINESS_TIMEOUT_SECONDS;
}
