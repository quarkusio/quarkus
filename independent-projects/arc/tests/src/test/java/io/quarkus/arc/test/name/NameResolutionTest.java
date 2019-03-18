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

package io.quarkus.arc.test.name;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class NameResolutionTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Bravo.class, Alpha.class);

    @Test
    public void testBeanNames() {
        assertTrue(Arc.container().instance("A").isAvailable());
        assertTrue(Arc.container().instance("bravo").isAvailable());
        assertEquals(12345, Arc.container().instance("bongo").get());
        assertEquals("bing", Arc.container().instance("producedBing").get());
        assertEquals(1, Arc.container().beanManager().getBeans("bongo").size());
    }

    @Named("A")
    @Singleton
    static class Alpha {

    }

    @Named
    @Dependent
    static class Bravo {

        @Named // -> defaulted to "producedBing"
        @Produces
        String producedBing = "bing";

        @Named // -> defaulted to "bongo"
        @Produces
        Integer getBongo() {
            return 12345;
        }

    }

}
