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

package io.quarkus.arc.test.producer.discovery;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.inject.Produces;
import org.junit.Rule;
import org.junit.Test;

public class ProducerOnClassWithoutBeanDefiningAnnotationTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(StringProducerMethod.class, IntegerProducerField.class);

    @Test
    public void testObserver() {
        assertEquals("foo", Arc.container().instance(String.class).get());
        assertEquals(Integer.valueOf(10), Arc.container().instance(Integer.class).get());
    }

    static class StringProducerMethod {

        @Produces
        String observeString() {
            return "foo";
        }

    }

    static class IntegerProducerField {

        @Produces
        Integer foo = 10;

    }

}
