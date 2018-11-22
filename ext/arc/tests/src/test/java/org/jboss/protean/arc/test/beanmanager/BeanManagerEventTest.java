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

package org.jboss.protean.arc.test.beanmanager;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class BeanManagerEventTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(StringObserver.class);

    @Test
    public void testGetEvent() {
        BeanManager beanManager = Arc.container().beanManager();
        beanManager.getEvent().fire("foo");
        assertEquals("foo", StringObserver.OBSERVED.get());
    }

    @Dependent
    static class StringObserver {

        private static final AtomicReference<String> OBSERVED = new AtomicReference<String>();

        void observeString(@Observes String value) {
            OBSERVED.set(value);
        }

    }

}
