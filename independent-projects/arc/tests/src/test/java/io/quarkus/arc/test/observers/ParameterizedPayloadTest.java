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

package io.quarkus.arc.test.observers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class ParameterizedPayloadTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(ListObserver.class, ListProducer.class);

    @Test
    public void testObservers() {
        ListProducer producer = Arc.container().instance(ListProducer.class).get();
        ListObserver observer = Arc.container().instance(ListObserver.class).get();
        List<Integer> intList = new ArrayList<>();
        intList.add(1);
        producer.produceInt(intList);
        List<? extends Number> observedInt = observer.getIntList();
        assertNotNull(observedInt);
        assertEquals(1, observedInt.size());
        assertEquals(1, observedInt.get(0));

        List<String> strList = new ArrayList<>();
        strList.add("ping");
        producer.produceStr(strList);
        List<String> observedStr = observer.getStrList();
        assertNotNull(observedStr);
        assertEquals(1, observedStr.size());
        assertEquals("ping", observedStr.get(0));
    }

    @Singleton
    static class ListObserver {

        private AtomicReference<List<? extends Number>> intList;

        private AtomicReference<List<String>> strList;

        @PostConstruct
        void init() {
            intList = new AtomicReference<>();
            strList = new AtomicReference<>();
        }

        <T extends List<? extends Number>> void observeIntList(@Observes T value) {
            intList.set(value);
        }

        List<? extends Number> getIntList() {
            return intList.get();
        }

        void observeStrList(@Observes List<String> value) {
            strList.set(value);
        }

        List<String> getStrList() {
            return strList.get();
        }

    }

    @Dependent
    static class ListProducer {

        @Inject
        Event<List<Integer>> intEvent;

        @Inject
        Event<Collection<String>> strEvent;

        void produceInt(List<Integer> value) {
            intEvent.fire(value);
        }

        void produceStr(Collection<String> value) {
            strEvent.fire(value);
        }

    }

}
