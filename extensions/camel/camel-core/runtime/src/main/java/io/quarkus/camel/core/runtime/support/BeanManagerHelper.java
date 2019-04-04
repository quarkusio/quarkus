/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.camel.core.runtime.support;

import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;

@Vetoed
final class BeanManagerHelper {

    private BeanManagerHelper() {
    }

    static <T> Set<T> getReferencesByType(Class<T> type, Annotation... qualifiers) {
        BeanManager manager = Arc.container().beanManager();
        return getReferencesByType(manager, type, qualifiers);
    }

    static <T> Optional<T> getReferenceByName(String name, Class<T> type) {
        BeanManager manager = Arc.container().beanManager();
        return getReferenceByName(manager, name, type);
    }

    static <T> Map<String, T> getReferencesByTypeWithName(Class<T> type, Annotation... qualifiers) {
        BeanManager manager = Arc.container().beanManager();
        return getReferencesByTypeWithName(manager, type, qualifiers);
    }

    static <T> Set<T> getReferencesByType(BeanManager manager, Class<T> type, Annotation... qualifiers) {
        return manager.getBeans(type, qualifiers).stream()
                .map(bean -> getReference(manager, type, bean))
                .collect(toSet());
    }

    static <T> Optional<T> getReferenceByName(BeanManager manager, String name, Class<T> type) {
        return Optional.of(manager.getBeans(name))
                .<Bean<?>> map(manager::resolve)
                .map(bean -> getReference(manager, type, bean));
    }

    static <T> T getReference(BeanManager manager, Class<T> type, Bean<?> bean) {
        return type.cast(manager.getReference(bean, type, manager.createCreationalContext(bean)));
    }

    static <T> Map<String, T> getReferencesByTypeWithName(BeanManager manager, Class<T> type, Annotation... qualifiers) {
        return manager.getBeans(type, qualifiers).stream()
                .collect(Collectors.toMap(
                        BeanAttributes::getName,
                        b -> getReference(manager, type, b)));
    }

}
