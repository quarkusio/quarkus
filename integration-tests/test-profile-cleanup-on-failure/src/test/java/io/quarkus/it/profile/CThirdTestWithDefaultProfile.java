package io.quarkus.it.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Order(3)
class CThirdTestWithDefaultProfile {

    @Inject
    Limits limits;

    @Test
    void alsoSeesDefaultConfig() {
        assertEquals(100, limits.limit(), "app.limit should still be from application.properties");
    }
}
