package io.quarkus.it.extension.configurable;

import java.lang.annotation.*;

import jakarta.enterprise.inject.Stereotype;

import io.quarkus.test.common.QuarkusTestResource;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@QuarkusTestResource(ConfigurableLifecycleManager.class)
@Stereotype
public @interface CustomResourceWithAttribute {

    String value();

}
