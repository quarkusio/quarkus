package io.quarkus.google.cloud.functions.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.common.QuarkusTestResource;

/**
 * This annotation can be used to start a Google Cloud Function for a test. It must be configured with the type of
 * function used, {@link FunctionType}. If multiple functions exist in the same project, you <b>must</b> configure which
 * function to launch via the <code>functionName</code> attribute.
 *
 * @see CloudFunctionTestResource
 */
@QuarkusTestResource(value = CloudFunctionTestResource.class, restrictToAnnotatedClass = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithFunction {
    FunctionType value();

    String functionName() default "";
}
