package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class PriorityTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        assertIterableEquals(Arrays.asList("1", "2", "3", "4", "5", "6"), MyExtension.invocations);
    }

    public static class MyExtension implements BuildCompatibleExtension {
        private static final LinkedHashSet<String> invocations = new LinkedHashSet<>();

        @Discovery
        @Priority(10)
        public void first() {
            invocations.add("1");
        }

        @Discovery
        @Priority(20)
        public void second() {
            invocations.add("2");
        }

        @Enhancement(types = Object.class, withSubtypes = true)
        @Priority(15)
        public void third(ClassConfig clazz) {
            invocations.add("3");
        }

        @Registration(types = Object.class)
        @Priority(5)
        public void fourth(BeanInfo clazz) {
            invocations.add("4");
        }

        @Validation
        public void fifth() {
            invocations.add("5");
        }

        @Validation
        @Priority(100_000)
        public void sixth() {
            invocations.add("6");
        }
    }
}
