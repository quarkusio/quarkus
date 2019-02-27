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
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.Rule;
import org.junit.Test;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

public class InjectionPointMetadataTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Controller.class, Controlled.class);

    @SuppressWarnings("unchecked")
    @Test
    public void testInjectionPointMetadata() {
        ArcContainer arc = Arc.container();
        Controller controller = arc.instance(Controller.class).get();

        // Field
        InjectionPoint injectionPoint = controller.controlled.injectionPoint;
        assertNotNull(injectionPoint);
        assertEquals(Controlled.class, injectionPoint.getType());
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        assertEquals(1, qualifiers.size());
        assertEquals(Default.class, qualifiers.iterator().next().annotationType());
        Bean<?> bean = injectionPoint.getBean();
        assertNotNull(bean);
        assertTrue(bean.getTypes().stream().anyMatch(t -> t.equals(Controller.class)));
        assertNotNull(injectionPoint.getAnnotated());
        assertTrue(injectionPoint.getAnnotated() instanceof AnnotatedField);
        AnnotatedField<Controller> annotatedField = (AnnotatedField<Controller>) injectionPoint.getAnnotated();
        assertEquals("controlled", annotatedField.getJavaMember().getName());
        assertEquals(Controlled.class, annotatedField.getBaseType());
        assertTrue(annotatedField.isAnnotationPresent(Inject.class));
        assertTrue(annotatedField.getAnnotation(Singleton.class) == null);
        assertTrue(annotatedField.getAnnotations(Singleton.class).isEmpty());
        assertEquals(1, annotatedField.getAnnotations().size());

        // Method
        InjectionPoint methodInjectionPoint = controller.controlledMethod.injectionPoint;
        assertNotNull(methodInjectionPoint);
        assertEquals(Controlled.class, methodInjectionPoint.getType());
        assertTrue(methodInjectionPoint.getAnnotated() instanceof AnnotatedParameter);
        assertEquals(bean, methodInjectionPoint.getBean());
        AnnotatedParameter<Controller> methodParam = (AnnotatedParameter<Controller>) methodInjectionPoint.getAnnotated();
        assertEquals(0, methodParam.getPosition());
        assertEquals(Controller.class, methodParam.getDeclaringCallable().getJavaMember().getDeclaringClass());
        assertEquals("setControlled", methodParam.getDeclaringCallable().getJavaMember().getName());

        // Constructor
        InjectionPoint ctorInjectionPoint = controller.controlledCtor.injectionPoint;
        assertNotNull(ctorInjectionPoint);
        assertEquals(Controlled.class, methodInjectionPoint.getType());
        assertTrue(ctorInjectionPoint.getAnnotated() instanceof AnnotatedParameter);
        assertEquals(bean, ctorInjectionPoint.getBean());
        AnnotatedParameter<Controller> ctorParam = (AnnotatedParameter<Controller>) ctorInjectionPoint.getAnnotated();
        assertEquals(1, ctorParam.getPosition());
        assertTrue(ctorParam.isAnnotationPresent(Singleton.class));
        assertTrue(ctorParam.getAnnotation(Singleton.class) != null);
        assertTrue(!ctorParam.getAnnotations(Singleton.class).isEmpty());
        assertEquals(1, ctorParam.getAnnotations().size());
        assertTrue(ctorParam.getDeclaringCallable() instanceof AnnotatedConstructor);
        assertEquals(Controller.class, ctorParam.getDeclaringCallable().getJavaMember().getDeclaringClass());
    }

    @Singleton
    static class Controller {

        @Inject
        Controlled controlled;

        Controlled controlledMethod;

        Controlled controlledCtor;

        @Inject
        public Controller(BeanManager beanManager, @Singleton Controlled controlled) {
            this.controlledCtor = controlled;
        }

        @Inject
        void setControlled(Controlled controlled, BeanManager beanManager) {
            this.controlledMethod = controlled;
        }

    }

    @Dependent
    static class Controlled {

        @Inject
        InjectionPoint injectionPoint;

    }

}
