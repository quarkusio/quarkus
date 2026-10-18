package org.acme;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyQuarkusTest {
}
