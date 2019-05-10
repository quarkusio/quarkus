/**
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.apache.camel.NoSuchBeanException;
import org.apache.camel.spi.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;

/**
 * A {@link Map}-based registry.
 */
public class RuntimeRegistry extends HashMap<String, Map<Class<?>, Object>> implements Registry {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public void bind(String name, Object object) {
        bind(name, object.getClass(), object);
    }

    public void bind(String name, Class<?> clazz, Object object) {
        this.computeIfAbsent(name, k -> new HashMap<>()).put(clazz, object);
    }

    public Object lookupByName(String name) {
        return lookupByNameAndType(name, Object.class);
    }

    public <T> T lookupByNameAndType(String name, Class<T> type) {
        Optional<T> t = BeanManagerHelper.getReferenceByName(name, type);
        if (t.isPresent()) {
            return t.get();
        }
        Map<Class<?>, Object> map = this.get(name);
        if (map == null) {
            return null;
        }
        Object answer = map.get(type);
        if (answer == null) {
            for (Map.Entry<Class<?>, Object> entry : map.entrySet()) {
                if (type.isAssignableFrom(entry.getKey())) {
                    answer = entry.getValue();
                    break;
                }
            }
        }
        if (answer instanceof RuntimeValue) {
            log.debug("Creating {} for name {}", type.toString(), name);
            answer = ((RuntimeValue) answer).getValue();
        }
        try {
            return type.cast(answer);
        } catch (Throwable e) {
            String msg = "Found bean: " + name + " in SimpleRegistry: " + this
                    + " of type: " + answer.getClass().getName() + " expected type was: " + type;
            throw new NoSuchBeanException(name, msg, e);
        }
    }

    public <T> Map<String, T> findByTypeWithName(Class<T> type) {
        Map<String, T> result = new HashMap<>();
        for (Entry<String, Map<Class<?>, Object>> entry : entrySet()) {
            for (Object answer : entry.getValue().values()) {
                if (answer instanceof RuntimeValue) {
                    answer = ((RuntimeValue) answer).getValue();
                }
                if (type.isInstance(answer)) {
                    result.put(entry.getKey(), type.cast(answer));
                }
            }
        }
        result.putAll(BeanManagerHelper.getReferencesByTypeWithName(type));
        return result;
    }

    public <T> Set<T> findByType(Class<T> type) {
        Set<T> result = new HashSet<>();
        for (Entry<String, Map<Class<?>, Object>> entry : entrySet()) {
            for (Object answer : entry.getValue().values()) {
                if (answer instanceof RuntimeValue) {
                    answer = ((RuntimeValue) answer).getValue();
                }
                if (type.isInstance(answer)) {
                    result.add(type.cast(answer));
                }
            }
        }
        result.addAll(BeanManagerHelper.getReferencesByType(type));
        return result;
    }

}
