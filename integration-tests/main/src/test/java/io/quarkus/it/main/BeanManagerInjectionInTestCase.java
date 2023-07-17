package io.quarkus.it.main;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.ExternalService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BeanManagerInjectionInTestCase {

    @Inject
    BeanManager beanManager;

    @Test
    public void testInjection() {
        Assertions.assertNotNull(beanManager);
        Assertions.assertFalse(beanManager.getBeans(ExternalService.class).isEmpty());
    }
}
