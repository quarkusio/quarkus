package io.quarkus.example.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.example.arc.UnusedBean;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BeanOnlyInTestCase {

    @Inject
    UnusedBean unusedBean;

    @Test
    void assertBeanIsInjected() {
        assertNotNull(unusedBean);
    }
}
