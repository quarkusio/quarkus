package io.quarkus.it.main;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.Stereotype;
import jakarta.transaction.Transactional;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
@Transactional
public @interface TransactionalQuarkusTest {
}
