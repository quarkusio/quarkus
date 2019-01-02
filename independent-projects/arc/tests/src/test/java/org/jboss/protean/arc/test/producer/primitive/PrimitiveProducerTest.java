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

package org.jboss.protean.arc.test.producer.primitive;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class PrimitiveProducerTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(IntProducer.class, LongProducer.class, PrimitiveConsumer.class);

    @Test
    public void testPrimitiveProducers() {
        assertEquals(Long.valueOf(10), Arc.container().instance(Long.class).get());
        assertEquals(Integer.valueOf(10), Arc.container().instance(Integer.class).get());
        PrimitiveConsumer consumer = Arc.container().instance(PrimitiveConsumer.class).get();
        assertEquals(10, consumer.getIntFoo());
        assertEquals(10l, consumer.getLongFoo());
    }

    @Dependent
    static class IntProducer {

        @Produces
        int foo = 10;

    }

    @Dependent
    static class LongProducer {

        @Produces
        long foo() {
            return 10;
        }

    }
    
    @Singleton
    static class PrimitiveConsumer {
        
        @Inject
        int intFoo;
        
        @Inject
        long longFoo;

        int getIntFoo() {
            return intFoo;
        }

        long getLongFoo() {
            return longFoo;
        }
        
    }
}
