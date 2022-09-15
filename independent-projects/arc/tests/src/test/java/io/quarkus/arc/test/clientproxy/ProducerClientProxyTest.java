package io.quarkus.arc.test.clientproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ProducerClientProxyTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Producer.class, Product.class, Product2.class);

    @Test
    public void testProducer() throws IOException {
        InstanceHandle<Product> instance = Arc.container().instance(Product.class);
        assertEquals(Long.valueOf(1), instance.get().get(Long.valueOf(1)));
        assertEquals(Long.valueOf(1), instance.get().getDefault(Long.valueOf(1)));

        InstanceHandle<Product2> instance2 = Arc.container().instance(Product2.class);
        assertEquals(Long.valueOf(1), instance2.get().get(Long.valueOf(1)));
        assertEquals(Long.valueOf(1), instance2.get().getDefault(Long.valueOf(1)));

        InstanceHandle<FunctionChild> supInstance = Arc.container().instance(FunctionChild.class);
        assertEquals("hi stu", supInstance.get().apply("stu"));
    }

    @Singleton
    static class Producer {

        /**
         * Test the case when the return type is a class, and the getDefault
         * method is defined in a super interface
         */
        @Produces
        @ApplicationScoped
        MyProduct produce() {
            return new MyProduct();
        };

        /**
         * Test the case when the return type is an interface, and the getDefault
         * method is defined in a super interface
         */
        @Produces
        @ApplicationScoped
        Product2 produce2() {
            return new MyProduct2();
        };

        @Produces
        @ApplicationScoped
        FunctionChild produceSupplier() {
            return new FunctionChild() {

            };
        }
    }

    static class MyProduct implements Product {
        @Override
        public <T extends Number> T get(T number) throws IOException {
            return number;
        }
    }

    static class MyProduct2 implements Product2 {
        @Override
        public <T extends Number> T get(T number) throws IOException {
            return number;
        }
    }

    interface Product {

        <T extends Number> T get(T number) throws IOException;

        default <T extends Number> T getDefault(T number) throws IOException {
            return number;
        }
    }

    interface Product2 extends Product2Interface {

    }

    interface Product2Interface {
        <T extends Number> T get(T number) throws IOException;

        default <T extends Number> T getDefault(T number) throws IOException {
            return number;
        }
    }

    interface FunctionSuper extends Function<String, String> {

    }

    interface FunctionChild extends FunctionSuper {
        @Override
        default String apply(String val) {
            return "hi " + val;
        }
    }
}
