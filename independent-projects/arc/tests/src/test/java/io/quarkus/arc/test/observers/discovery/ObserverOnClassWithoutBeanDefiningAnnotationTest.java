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

package io.quarkus.arc.test.observers.discovery;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class ObserverOnClassWithoutBeanDefiningAnnotationTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(StringObserver.class);

    @Test
    public void testObserver() {
        BeanManager beanManager = Arc.container().beanManager();
        beanManager.getEvent().fire("foo");
        beanManager.getEvent().fire("ping");
        assertEquals(2, StringObserver.EVENTS.size());
    }

    static class StringObserver {

        private static List<String> EVENTS = new CopyOnWriteArrayList<>();

        void observeString(@Observes String value) {
            EVENTS.add(value);
        }

    }

}
