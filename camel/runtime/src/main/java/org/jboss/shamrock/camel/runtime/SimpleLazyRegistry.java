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
package org.jboss.shamrock.camel.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.camel.NoSuchBeanException;
import org.apache.camel.spi.Registry;
import org.jboss.shamrock.runtime.InjectionInstance;

/**
 * A {@link Map}-based registry.
 */
public class SimpleLazyRegistry extends HashMap<String, Object> implements Registry {

    public Object lookupByName(String name) {
        Object result = get(name);
        if (result instanceof InjectionInstance) {
            result = ((InjectionInstance) result).newInstance();
            put(name, result);
        }
        return result;
    }

    public <T> T lookupByNameAndType(String name, Class<T> type) {
        Object answer = lookupByName(name);

        // just to be safe
        if (answer == null) {
            return null;
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
        for (Entry<String, Object> entry : entrySet()) {
            if (type.isInstance(entry.getValue())) {
                result.put(entry.getKey(), type.cast(entry.getValue()));
            }
        }
        return result;
    }

    public <T> Set<T> findByType(Class<T> type) {
        Set<T> result = new HashSet<>();
        for (Entry<String, Object> entry : entrySet()) {
            if (type.isInstance(entry.getValue())) {
                result.add(type.cast(entry.getValue()));
            }
        }
        return result;
    }

}
