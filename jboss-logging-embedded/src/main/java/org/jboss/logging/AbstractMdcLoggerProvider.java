/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2010 Red Hat, Inc.
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

package org.jboss.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractMdcLoggerProvider extends AbstractLoggerProvider {

    private final ThreadLocal<Map<String, Object>> mdcMap = new ThreadLocal<Map<String, Object>>();

    public void clearMdc() {
        final Map<String, Object> map = mdcMap.get();
        if (map != null) {
            map.clear();
        }
    }

    public Object getMdc(String key) {
        return mdcMap.get() == null ? null : mdcMap.get().get(key);
    }

    public Map<String, Object> getMdcMap() {
        final Map<String, Object> map = mdcMap.get();
        return map == null ? Collections.<String, Object>emptyMap() : map;
    }

    public Object putMdc(String key, Object value) {
        Map<String, Object> map = mdcMap.get();
        if (map == null) {
            map = new HashMap<String, Object>();
            mdcMap.set(map);
        }
        return map.put(key, value);
    }

    public void removeMdc(String key) {
        Map<String, Object> map = mdcMap.get();
        if (map == null)
            return;
        map.remove(key);
    }
}
