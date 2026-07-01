package io.quarkus.test.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MultiLevelNestedUT {

    @Inject
    NestedBean bean;

    @Nested
    class OuterGroup {

        @Nested
        class InnerGroup {

            @Test
            void testInjection() {
                assertEquals("hello", bean.ping());
            }
        }
    }
}
