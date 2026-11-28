package io.quarkus.it.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.Stereotype;

import io.quarkus.test.common.QuarkusTestResource;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@QuarkusTestResource.List({
        @QuarkusTestResource(value = LifecycleManager.class, restrictToAnnotatedClass = true)
})
@Stereotype
public @interface CustomResourceWithList {

}
