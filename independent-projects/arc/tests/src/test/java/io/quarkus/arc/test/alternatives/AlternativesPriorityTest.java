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

package io.quarkus.arc.test.alternatives;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.function.Supplier;
import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class AlternativesPriorityTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Bravo.class, Charlie.class);

    @SuppressWarnings("serial")
    @Test
    public void testAlternativePriority() {
        assertEquals(Charlie.class.getName(), Arc.container().instance(new TypeLiteral<Supplier<String>>() {
        }).get().get());
    }

    @Singleton
    static class Alpha implements Supplier<String> {

        @Override
        public String get() {
            return getClass().getName();
        }

    }

    @Alternative
    @Priority(1)
    @Singleton
    static class Bravo implements Supplier<String> {

        @Override
        public String get() {
            return getClass().getName();
        }

    }

    @Alternative
    @Priority(10)
    @Singleton
    static class Charlie {

        @Produces
        Supplier<String> supplier = () -> Charlie.class.getName();

    }

}
