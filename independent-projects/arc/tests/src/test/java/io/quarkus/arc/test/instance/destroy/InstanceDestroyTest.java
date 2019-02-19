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

package org.jboss.quarkus.arc.test.instance.destroy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.quarkus.arc.Arc;
import org.jboss.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class InstanceDestroyTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Washcloth.class);

    @Test
    public void testDestroy() {
        assertFalse(Washcloth.DESTROYED.get());
        Arc.container().instance(Alpha.class).get().doSomething();
        assertTrue(Washcloth.DESTROYED.get());
    }

    @Singleton
    static class Alpha {

        @Inject
        Instance<Washcloth> instance;

        void doSomething() {
            Washcloth washcloth = instance.get();
            washcloth.wash();
            instance.destroy(washcloth);
        }

    }

    @Dependent
    static class Washcloth {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        void wash() {
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

}
