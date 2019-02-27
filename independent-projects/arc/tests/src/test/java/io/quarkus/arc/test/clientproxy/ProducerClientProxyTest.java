/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.test.clientproxy;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

import org.junit.Rule;
import org.junit.Test;

public class ProducerClientProxyTest {

    @Rule
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
        default <T extends Number> T getDefault(T number) throws IOException{
            return number;
        }
    }

    interface Product2 extends Product2Interface {

    }

    interface Product2Interface {
        <T extends Number> T get(T number) throws IOException;
        default <T extends Number> T getDefault(T number) throws IOException{
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
