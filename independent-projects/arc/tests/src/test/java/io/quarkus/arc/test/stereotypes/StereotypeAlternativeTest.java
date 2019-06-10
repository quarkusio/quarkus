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

package io.quarkus.arc.test.stereotypes;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import org.junit.Rule;
import org.junit.Test;

public class StereotypeAlternativeTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(BeAlternative.class, BeAlternativeWithPriority.class,
            NonAlternative.class, IamAlternative.class, NotAtAllAlternative.class, IamAlternativeWithPriority.class);

    @Test
    public void testStereotype() {
        assertEquals("OK", Arc.container().instance(NonAlternative.class).get().getId());
        assertEquals("OK", Arc.container().instance(NotAtAllAlternative.class).get().getId());
    }

    @Alternative
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeAlternative {
    }

    @Priority(1)
    @Alternative
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeAlternativeWithPriority {
    }

    @Dependent
    static class NonAlternative {

        public String getId() {
            return "NOK";
        }

    }

    @Priority(1)
    @BeAlternative
    static class IamAlternative extends NonAlternative {

        @Override
        public String getId() {
            return "OK";
        }

    }

    @Dependent
    static class NotAtAllAlternative {

        public String getId() {
            return "NOK";
        }

    }

    @BeAlternativeWithPriority
    static class IamAlternativeWithPriority extends NotAtAllAlternative {

        @Override
        public String getId() {
            return "OK";
        }

    }

}
