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
import static org.junit.Assert.assertNotEquals;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Typed;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class StereotypeScopeTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(ModelBean.class, ApplicationModelBean.class);

    @Test
    public void testStereotype() {
        ArcContainer container = Arc.container();
        String modelBean1Id;
        String appModelBean1Id;

        container.requestContext().activate();
        modelBean1Id = Arc.container().instance(ModelBean.class).get().getId();
        appModelBean1Id = Arc.container().instance(ApplicationModelBean.class).get().getId();
        container.requestContext().deactivate();

        container.requestContext().activate();
        assertNotEquals(modelBean1Id, Arc.container().instance(ModelBean.class).get().getId());
        assertEquals(appModelBean1Id, Arc.container().instance(ApplicationModelBean.class).get().getId());
        container.requestContext().deactivate();
    }

    @Model
    static class ModelBean {

        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        public String getId() {
            return id;
        }

    }

    @Typed(ApplicationModelBean.class)
    @ApplicationScoped
    @Model
    static class ApplicationModelBean extends ModelBean {

    }

}
