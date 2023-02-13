package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

public class SameBeanInstance {

    public interface UnderTest {
        void method1();
    }

    public interface I3 {
        void method3();
    }

    public interface I2 {
        void method2();
    }

    public interface I4 {
        void method4();
    }

    @ApplicationScoped
    public static class C1 implements UnderTest {
        @Inject
        I2 i2;

        @Inject
        I3 i3;

        @Inject
        I4 i4;

        @Override
        public void method1() {
            i2.method2();
            i3.method3();
        }
    }

    @ApplicationScoped
    public static class C2 implements I2, I3 {
        @Override
        public void method2() {

        }

        @Override
        public void method3() {

        }
    }

    @ApplicationScoped
    public static class C3 implements I4 {

        @Override
        public void method4() {

        }
    }
}
