package io.quarkus.it.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.Stereotype;

import io.quarkus.test.common.WithTestResource;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@WithTestResource(value = LifecycleManager.class, restrictToAnnotatedClass = false)
@Stereotype
public @interface CustomResource {

}
