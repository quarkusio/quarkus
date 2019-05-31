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

package io.quarkus.arc.test.observers.ifexists;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import org.junit.Rule;
import org.junit.Test;

public class ReceptionIfExistsTest {

    static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    @Rule
    public ArcTestContainer container = new ArcTestContainer(DependentObserver.class, RequestScopedObserver.class);

    @Test
    public void testObserver() {
        Arc.container().beanManager().fireEvent("foo");
        assertEquals(1, EVENTS.size());
        assertEquals(DependentObserver.class.getName() + "foo", EVENTS.get(0));
    }

    @RequestScoped
    static class RequestScopedObserver {

        void observeString(@Observes(notifyObserver = Reception.IF_EXISTS) String value) {
            EVENTS.add(RequestScopedObserver.class.getName() + value);
        }

    }

    @Dependent
    static class DependentObserver {

        void observeString(@Observes String value) {
            EVENTS.add(DependentObserver.class.getName() + value);
        }

    }

}
