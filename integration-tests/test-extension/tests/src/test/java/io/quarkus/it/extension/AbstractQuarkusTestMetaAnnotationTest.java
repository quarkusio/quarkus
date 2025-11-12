package io.quarkus.it.extension;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractQuarkusTestMetaAnnotationTest {

    @Inject
    MyTestBean bean;

    @Test
    void testInjectedBean() {
        Assertions.assertEquals("foo", bean.foo());
    }

    @ApplicationScoped
    public static class MyTestBean {

        public String foo() {
            return "foo";
        }

    }

}
