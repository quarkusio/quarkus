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

package io.quarkus.arc.test.instance.frombean;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class InstanceFromBeanTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Alpha.class);

    @SuppressWarnings("unchecked")
    @Test
    public void testDestroy() {
        InjectableBean<Alpha> bean1 = (InjectableBean<Alpha>) Arc.container().beanManager().getBeans(Alpha.class).iterator()
                .next();
        InjectableBean<Alpha> bean2 = Arc.container().bean(bean1.getIdentifier());
        assertEquals(bean1, bean2);
        assertEquals(Arc.container().instance(bean2).get().getId(), Arc.container().instance(bean2).get().getId());
    }

    @Singleton
    static class Alpha {

        private String id;

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

        String getId() {
            return id;
        }

    }

}
