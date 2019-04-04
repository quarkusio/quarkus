package io.quarkus.example.test;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.wildfly.common.Assert;

import io.quarkus.example.arc.UnusedBean;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests JUnit 5 extension when test lifecycle is PER_CLASS. This means extension events get fired in slightly different
 * order and Quarkus/Arc bootstrap and instance injection have to account for that.
 *
 * Test verifies that bootstrap works and that you can use injection even in before/after methods.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JUnit5PerClassLifecycleTest {

    // any IP just to verify it was performed
    @Inject
    UnusedBean bean;

    @BeforeEach
    public void beforeEach() {
        Assertions.assertNotNull(bean);
        Assert.assertNotNull(bean.getInjectionPoint());
    }

    @BeforeAll
    public void beforeAll() {
        Assertions.assertNotNull(bean);
        Assert.assertNotNull(bean.getInjectionPoint());
    }

    @AfterEach
    public void afterEach() {
        Assertions.assertNotNull(bean);
        Assert.assertNotNull(bean.getInjectionPoint());
    }

    @AfterAll
    public void afterAll() {
        Assertions.assertNotNull(bean);
        Assert.assertNotNull(bean.getInjectionPoint());
    }

    @Test
    public void testQuarkusWasBootedAndInjectionWorks() {
        Assertions.assertNotNull(bean);
        Assert.assertNotNull(bean.getInjectionPoint());
    }
}
