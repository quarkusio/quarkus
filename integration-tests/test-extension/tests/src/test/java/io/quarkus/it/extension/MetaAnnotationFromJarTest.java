package io.quarkus.it.extension;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.metaannotation.QuarkusTestFromJar;

/**
 * Reproducer for quarkus#56133: a composed {@code @QuarkusTest} meta-annotation defined in a
 * separate (Jandex-indexed) jar must register the test class as a CDI bean so its {@code @Inject}
 * fields resolve.
 */
@QuarkusTestFromJar
public class MetaAnnotationFromJarTest {

    @Inject
    MyJarTestBean bean;

    @Test
    void testInjectedBean() {
        Assertions.assertEquals("foo", bean.foo());
    }

    @ApplicationScoped
    public static class MyJarTestBean {

        public String foo() {
            return "foo";
        }

    }

}
