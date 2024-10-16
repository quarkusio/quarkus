package io.quarkus.test.component.paraminject;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.quarkus.test.component.QuarkusComponentTest;

@TestInstance(Lifecycle.PER_METHOD)
@QuarkusComponentTest
public class ParameterInjectionPerMethodLifecycleTest {

    static volatile String mySingletonId;

    @Order(1)
    @Test
    public void testSingleton1(MySingleton mySingleton) {
        mySingletonId = mySingleton.id;
    }

    @Order(2)
    @Test
    public void testSingleton2(MySingleton mySingleton) {
        // Each test method starts a different CDI container
        assertNotEquals(mySingletonId, mySingleton.id);
    }

    @Singleton
    public static class MySingleton {

        String id;

        @PostConstruct
        void initId() {
            id = UUID.randomUUID().toString();
        }
    }

}
