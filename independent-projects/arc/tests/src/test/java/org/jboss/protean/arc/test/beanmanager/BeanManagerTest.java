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

package org.jboss.protean.arc.test.beanmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class BeanManagerTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Legacy.class, AlternativeLegacy.class, Fool.class);

    @Test
    public void testGetBeans() {
        BeanManager beanManager = Arc.container().instance(Legacy.class).get().getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(Legacy.class);
        assertEquals(2, beans.size());
        assertEquals(AlternativeLegacy.class, beanManager.resolve(beans).getBeanClass());
    }

    @Test
    public void testGetReference() {
        Fool.DESTROYED.set(false);
        Legacy.DESTROYED.set(false);

        BeanManager beanManager = Arc.container().instance(Legacy.class).get().getBeanManager();

        Set<Bean<?>> foolBeans = beanManager.getBeans(Fool.class);
        assertEquals(1, foolBeans.size());
        @SuppressWarnings("unchecked")
        Bean<Fool> foolBean = (Bean<Fool>) foolBeans.iterator().next();
        Fool fool1 = (Fool) beanManager.getReference(foolBean, Fool.class, beanManager.createCreationalContext(foolBean));
        Arc.container().withinRequest(() -> assertEquals(fool1.getId(),
                ((Fool) beanManager.getReference(foolBean, Fool.class, beanManager.createCreationalContext(foolBean))).getId())).run();
        assertTrue(Fool.DESTROYED.get());

        Set<Bean<?>> legacyBeans = beanManager.getBeans(AlternativeLegacy.class);
        assertEquals(1, legacyBeans.size());
        @SuppressWarnings("unchecked")
        Bean<AlternativeLegacy> legacyBean = (Bean<AlternativeLegacy>) legacyBeans.iterator().next();
        CreationalContext<AlternativeLegacy> ctx = beanManager.createCreationalContext(legacyBean);
        Legacy legacy = (Legacy) beanManager.getReference(legacyBean, Legacy.class, ctx);
        assertNotNull(legacy.getBeanManager());
        ctx.release();
        assertTrue(Legacy.DESTROYED.get());
    }

    @Dependent
    static class Legacy {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @Inject
        BeanManager beanManager;

        public BeanManager getBeanManager() {
            return beanManager;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

    @Priority(1)
    @Alternative
    @Dependent
    static class AlternativeLegacy extends Legacy {

    }

    @RequestScoped
    static class Fool {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

        String getId() {
            return id;
        }

    }

}
