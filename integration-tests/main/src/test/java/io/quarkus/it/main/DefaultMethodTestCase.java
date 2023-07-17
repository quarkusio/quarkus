package io.quarkus.it.main;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DefaultMethodTestCase implements DefaultMethodInterface {

    @Nested
    class NestedTestClass implements DefaultMethodInterface {
    }

    interface InnerTestInterface {
        @Test
        default void testInnerTestInterface() {

        }
    }

    @Nested
    class InnerTestInterfaceTest implements InnerTestInterface {

    }

    @Nested
    class SomeTestInterfaceTest implements DefaultMethodInterface, InnerTestInterface {

    }

    interface NonTestInterface {
        default void simpleMethod() {

        }

        void abstractMethod();
    }

    @Nested
    class NonTestInterfaceTest implements NonTestInterface {

        @Override
        public void abstractMethod() {

        }
    }

    interface SuperTestInterface {
        @Test
        default void superTestMethod() {

        }
    }

    interface ChildTestInterface extends SuperTestInterface {
        @Test
        default void childTestMethod() {

        }
    }

    @Nested
    class HierarchyTest implements ChildTestInterface {

    }
}
