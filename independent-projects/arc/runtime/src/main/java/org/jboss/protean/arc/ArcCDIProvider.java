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
package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;
import javax.enterprise.util.TypeLiteral;

/**
 * 
 * @author Martin Kouba
 */
public class ArcCDIProvider implements CDIProvider {

    private final ArcCDI arcCDI;

    public ArcCDIProvider() {
        this.arcCDI = new ArcCDI();
    }

    @Override
    public CDI<Object> getCDI() {
        return arcCDI;
    }

    static class ArcCDI extends CDI<Object> {

        private final Instance<Object> instanceDelegate;

        public ArcCDI() {
            this.instanceDelegate = Arc.container()
                    .beanManager()
                    .createInstance();
        }

        @Override
        public Instance<Object> select(Annotation... qualifiers) {
            return instanceDelegate.select(qualifiers);
        }

        @Override
        public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
            return instanceDelegate.select(subtype, qualifiers);
        }

        @Override
        public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
            return instanceDelegate.select(subtype, qualifiers);
        }

        @Override
        public boolean isUnsatisfied() {
            return instanceDelegate.isUnsatisfied();
        }

        @Override
        public boolean isAmbiguous() {
            return instanceDelegate.isAmbiguous();
        }

        @Override
        public void destroy(Object instance) {
            this.instanceDelegate.destroy(instance);
        }

        @Override
        public Iterator<Object> iterator() {
            return instanceDelegate.iterator();
        }

        @Override
        public Object get() {
            return instanceDelegate.get();
        }

        @Override
        public BeanManager getBeanManager() {
            return Arc.container()
                    .beanManager();
        }

        void destroy() {
            ((InstanceImpl<?>) instanceDelegate).destroy();
        }

    }

}
