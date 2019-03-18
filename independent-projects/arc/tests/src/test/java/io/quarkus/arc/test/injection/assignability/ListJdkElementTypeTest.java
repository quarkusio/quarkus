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

package io.quarkus.arc.test.injection.assignability;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.Collections;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class ListJdkElementTypeTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(ListProducer.class, InjectList.class);

    @Test
    public void testInjection() {
        assertEquals(Integer.valueOf(1), Arc.container().instance(InjectList.class).get().getListOfNumbers().get(0));
    }

    @ApplicationScoped
    static class ListProducer {

        @Dependent
        @Produces
        List<Integer> produceListOfIntegers() {
            return Collections.singletonList(1);
        }

    }

    @ApplicationScoped
    static class InjectList {

        private List<? extends Number> listOfNumbers;

        @Inject
        private void setLists(List<? extends Number> listOfNumbers) {
            this.listOfNumbers = listOfNumbers;
        }

        List<? extends Number> getListOfNumbers() {
            return listOfNumbers;
        }

    }
}
