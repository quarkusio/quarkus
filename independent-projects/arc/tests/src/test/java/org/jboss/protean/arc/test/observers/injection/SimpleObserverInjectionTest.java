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

package org.jboss.protean.arc.test.observers.injection;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class SimpleObserverInjectionTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Fool.class, StringObserver.class);

    @Test
    public void testObserverInjection() {
        AtomicReference<String> msg = new AtomicReference<String>();
        Fool.DESTROYED.set(false);
        Arc.container().beanManager().fireEvent(msg);
        String id1 = msg.get();
        assertNotNull(id1);
        assertTrue(Fool.DESTROYED.get());
        Fool.DESTROYED.set(false);
        Arc.container().beanManager().fireEvent(msg);
        assertNotEquals(id1, msg.get());
        assertTrue(Fool.DESTROYED.get());
    }

    @Singleton
    static class StringObserver {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        void observeString(@Observes AtomicReference value, Fool fool) {
            value.set(fool.id);
        }

    }

    @Dependent
    static class Fool {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }
    }

}

