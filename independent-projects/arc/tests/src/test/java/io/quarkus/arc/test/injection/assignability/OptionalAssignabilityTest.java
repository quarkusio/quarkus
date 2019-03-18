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
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class OptionalAssignabilityTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(OptionalProducer.class, InjectOptionals.class);

    @Test
    public void testInjection() {
        assertEquals(Integer.valueOf(10), Arc.container().instance(InjectOptionals.class).get().getAge());
    }

    @ApplicationScoped
    static class OptionalProducer {

        @SuppressWarnings("unchecked")
        @Dependent
        @Produces
        <T> Optional<T> produceOptional(InjectionPoint injectionPoint) {
            return (Optional<T>) Optional.of(10);
        }

    }

    @ApplicationScoped
    static class InjectOptionals {

        private Integer age;

        @Inject
        private void setOptionals(Optional<Integer> age) {
            this.age = age.orElse(1);
        }

        Integer getAge() {
            return age;
        }

    }
}
