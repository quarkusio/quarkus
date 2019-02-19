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

package org.jboss.quarkus.arc.test.observers.ordering;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.quarkus.arc.Arc;
import org.jboss.quarkus.arc.ArcContainer;
import org.jboss.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class ObserverOrderingTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, StringObserver.class);

    @Test
    public void testObservers() {
        ArcContainer container = Arc.container();
        StringProducer producer = container.instance(StringProducer.class).get();
        StringObserver observer = container.instance(StringObserver.class).get();
        producer.produce("sorted");
        List<String> events = observer.getEvents();
        assertEquals(3, events.size());
        assertEquals("1sorted", events.get(0));
        assertEquals("10sorted", events.get(1));
        assertEquals("1000sorted", events.get(2));
    }

    @Singleton
    static class StringObserver {

        private List<String> events;

        @PostConstruct
        void init() {
            events = new CopyOnWriteArrayList<>();
        }

        void observe10(@Observes @Priority(10) String value) {
            events.add("10" + value);
        }

        void observe1(@Observes @Priority(1) String value) {
            events.add("1" + value);
        }

        void observe1000(@Observes @Priority(1000) String value) {
            events.add("1000" + value);
        }

        List<String> getEvents() {
            return events;
        }

    }

    @Dependent
    static class StringProducer {

        @Inject
        Event<String> event;

        void produce(String value) {
            event.fire(value);
        }

    }

}
