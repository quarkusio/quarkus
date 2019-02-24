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

package io.quarkus.arc.test.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class InjectionPointMetadataTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Controller.class, Controlled.class);

    @Test
    public void testInjectionPointMetadata() {
        ArcContainer arc = Arc.container();
        Controller controller = arc.instance(Controller.class).get();
        InjectionPoint injectionPoint = controller.controlled.injectionPoint;
        assertNotNull(injectionPoint);
        assertEquals(Controlled.class, injectionPoint.getType());
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        assertEquals(1, qualifiers.size());
        assertEquals(Default.class, qualifiers.iterator().next().annotationType());
        Bean<?> bean = injectionPoint.getBean();
        assertNotNull(bean);
        assertTrue(bean.getTypes()
                .stream()
                .anyMatch(t -> t.equals(Controller.class)));
        Annotated annotated = injectionPoint.getAnnotated();
        assertNotNull(annotated);
        assertEquals(Controlled.class, annotated.getBaseType());
    }

    @Singleton
    static class Controller {

        @Inject
        Controlled controlled;

    }

    @Dependent
    static class Controlled {

        @Inject
        InjectionPoint injectionPoint;

    }

}
