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
import java.util.Map;

/**
 * Mapped diagnostic context. Each log provider implementation may behave different.
 */
public final class MDC {

    private MDC() {
    }

    /**
     * Puts the value onto the context.
     *
     * @param key the key for the value
     * @param val the value
     *
     * @return the previous value set or {@code null} if no value was set
     */
    public static Object put(String key, Object val) {
        return LoggerProviders.PROVIDER.putMdc(key, val);
    }

    /**
     * Returns the value for the key or {@code null} if no value was found.
     *
     * @param key the key to lookup the value for
     *
     * @return the value or {@code null} if not found
     */
    public static Object get(String key) {
        return LoggerProviders.PROVIDER.getMdc(key);
    }

    /**
     * Removes the value from the context.
     *
     * @param key the key of the value to remove
     */
    public static void remove(String key) {
        LoggerProviders.PROVIDER.removeMdc(key);
    }

    /**
     * Returns the map from the context.
     *
     * <p>
     * Note that in most implementations this is an expensive operation and should be used sparingly.
     * </p>
     *
     * @return the map from the context or an {@linkplain Collections#emptyMap() empty map} if the context is {@code
     * null}
     */
    public static Map<String, Object> getMap() {
        return LoggerProviders.PROVIDER.getMdcMap();
    }

    /**
     * Clears the message diagnostics context.
     */
    public static void clear() {
        LoggerProviders.PROVIDER.clearMdc();
    }
}
