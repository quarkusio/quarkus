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

package io.quarkus.arc.processor;

import java.util.List;

import org.jboss.jandex.IndexView;

/**
 * Build-time extension point.
 *
 * @author Martin Kouba
 */
public interface BuildExtension {

    static final int DEFAULT_PRIORITY = 1000;

    static int compare(BuildExtension e1, BuildExtension e2) {
        return Integer.compare(e2.getPriority(), e1.getPriority());
    }

    /**
     * Processors with higher priority are called first.
     *
     * @return the priority
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * Initialize self.
     *
     * @param buildContext
     * @return {@code true} if the extension should be put into service, @{code false} otherwise
     */
    default boolean initialize(BuildContext buildContext) {
        return true;
    }

    interface BuildContext {

        <V> V get(Key<V> key);

        <V> V put(Key<V> key, V value);

    }

    interface Key<T> {

        // Built-in keys
        static String BUILT_IN_PREFIX = BuildExtension.class.getPackage().getName() + ".";
        static Key<IndexView> INDEX = new SimpleKey<>(BUILT_IN_PREFIX + "index");
        static Key<List<InjectionPointInfo>> INJECTION_POINTS = new SimpleKey<>(BUILT_IN_PREFIX + "injectionPoints");
        static Key<List<BeanInfo>> BEANS = new SimpleKey<>(BUILT_IN_PREFIX + "beans");
        static Key<List<ObserverInfo>> OBSERVERS = new SimpleKey<>(BUILT_IN_PREFIX + "observers");
        static Key<AnnotationStore> ANNOTATION_STORE = new SimpleKey<>(BUILT_IN_PREFIX + "annotationStore");

        String asString();
    }

    public static class SimpleKey<V> implements Key<V> {

        private final String str;

        public SimpleKey(String str) {
            this.str = str;
        }

        @Override
        public String asString() {
            return str;
        }

    }

}
