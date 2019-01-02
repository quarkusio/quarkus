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
