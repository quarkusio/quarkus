package io.quarkus.it.extension;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.metaannotation.QuarkusTestExtensionFromJar;

/**
 * Companion to {@link MetaAnnotationFromJarTest} that exercises the
 * {@code @ExtendWith(QuarkusTestExtension.class)} discovery branch with a meta-annotation defined in a
 * separate (Jandex-indexed) jar (quarkus#56133).
 */
@QuarkusTestExtensionFromJar
public class ExtendWithFromJarTest {

    @Inject
    MyExtendWithJarBean bean;

    @Test
    void testInjectedBean() {
        Assertions.assertEquals("foo", bean.foo());
    }

    @ApplicationScoped
    public static class MyExtendWithJarBean {

        public String foo() {
            return "foo";
        }

    }

}
