package io.quarkus.hibernate.orm.panache.deployment.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.PanacheEntity_;

public class PanacheEntityMetaModelTest {
    @Test
    public void testMetaModelExistence() {
        Assertions.assertEquals("id", PanacheEntity_.ID);
    }
}
