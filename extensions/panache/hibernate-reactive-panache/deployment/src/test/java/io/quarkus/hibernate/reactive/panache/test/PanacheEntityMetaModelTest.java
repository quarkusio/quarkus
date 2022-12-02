package io.quarkus.hibernate.reactive.panache.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.panache.PanacheEntity_;

public class PanacheEntityMetaModelTest {
    @Test
    public void testMetaModelExistence() {
        Assertions.assertEquals("id", PanacheEntity_.ID);
    }
}
