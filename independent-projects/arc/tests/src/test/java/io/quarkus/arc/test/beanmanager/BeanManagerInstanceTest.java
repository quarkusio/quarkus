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

package io.quarkus.arc.test.beanmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class BeanManagerInstanceTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(FuuService.class);

    @Test
    public void testGetEvent() {
        BeanManager beanManager = Arc.container()
                .beanManager();
        Instance<FuuService> instance = beanManager.createInstance()
                .select(FuuService.class);
        assertTrue(instance.isResolvable());
        assertEquals(10, instance.get().age);
    }

    @Dependent
    static class FuuService {

        int age;

        @PostConstruct
        void init() {
            age = 10;
        }

    }

}
