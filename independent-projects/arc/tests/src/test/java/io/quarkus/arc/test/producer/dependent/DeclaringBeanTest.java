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

package io.quarkus.arc.test.producer.dependent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class DeclaringBeanTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(ListProducer.class, StringProducer.class, LongProducer.class);

    @SuppressWarnings("serial")
    @Test
    public void testDependendDestroyedProducerMethod() {
        TypeLiteral<List<String>> literal = new TypeLiteral<List<String>>() {
        };
        assertFalse(ListProducer.DESTROYED.get());
        List<String> list1 = Arc.container().instance(literal).get();
        // @Dependent contextual instance created to receive a producer method is destroyed when the invocation completes
        assertTrue(ListProducer.DESTROYED.get());
        assertNotEquals(list1, Arc.container().instance(literal).get());
    }

    @Test
    public void testDependendDestroyedProducerField() {
        assertFalse(StringProducer.DESTROYED.get());
        String string1 = Arc.container().instance(String.class).get();
        // @Dependent contextual instance created to receive a producer method is destroyed when the invocation completes
        assertTrue(StringProducer.DESTROYED.get());
        assertNotEquals(string1, Arc.container().instance(String.class).get());
    }

    @Test
    public void testSingletonNotDestroyed() {
        assertFalse(LongProducer.DESTROYED.get());
        Long long1 = Arc.container().instance(Long.class).get();
        assertFalse(LongProducer.DESTROYED.get());
        assertEquals(long1, Arc.container().instance(Long.class).get());
    }

    @Dependent
    static class ListProducer {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        @Produces
        List<String> produce() {
            List<String> list = new ArrayList<>();
            list.add(toString());
            return list;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

    @Dependent
    static class StringProducer {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        @Produces
        String produce = toString();

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

    @Singleton
    static class LongProducer {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        @Produces
        Long produce = System.currentTimeMillis();

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }
}
