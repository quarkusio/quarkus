package io.quarkus.test.kubernetes.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(value = DisposableNamespaceTestResource.class, restrictToAnnotatedClass = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithDisposableNamespace {
    boolean preserveNamespaceOnError() default false;

    int secondsToWaitForNamespaceDeletion() default 0;

    String namespace() default AnnotationConstants.UNSET_STRING_VALUE;
}
