package org.jboss.protean.arc.test.clientproxy;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class ProducerClientProxyTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Producer.class, Product.class);

    @Test
    public void testProducer() throws IOException {
        assertEquals(Long.valueOf(1), Arc.container().instance(Product.class).get().get(Long.valueOf(1)));
    }

    @Singleton
    static class Producer {

        @Produces
        @ApplicationScoped
        Product produce() {
            return new Product() {

                @Override
                public <T extends Number> T get(T number) throws IOException {
                    return number;
                }

            };
        }

    }

    interface Product {

        <T extends Number> T get(T number) throws IOException;

    }
}
