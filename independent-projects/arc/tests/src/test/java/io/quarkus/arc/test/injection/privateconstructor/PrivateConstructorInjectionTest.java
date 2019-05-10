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

package io.quarkus.arc.test.injection.privateconstructor;

import static org.junit.Assert.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class PrivateConstructorInjectionTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Head.class, CombineHarvester.class);

    @Test
    public void testInjection() {
        assertNotNull(Arc.container().instance(CombineHarvester.class).get().getHead());
    }

    @Dependent
    static class Head {

    }

    @Singleton
    static class CombineHarvester {

        private final Head head;

        @Inject
        private CombineHarvester(Head head) {
            this.head = head;
        }

        public Head getHead() {
            return head;
        }

    }
}
