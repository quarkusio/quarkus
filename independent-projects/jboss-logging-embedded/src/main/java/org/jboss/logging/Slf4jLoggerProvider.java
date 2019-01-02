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

package org.jboss.logging;

import java.util.Collections;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LocationAwareLogger;

final class Slf4jLoggerProvider extends AbstractLoggerProvider implements LoggerProvider {

    public Logger getLogger(final String name) {
        org.slf4j.Logger l = LoggerFactory.getLogger(name);
        try {
            return new Slf4jLocationAwareLogger(name, (LocationAwareLogger) l);
        } catch (Throwable ignored) {
        }
        return new Slf4jLogger(name, l);
    }

    public void clearMdc() {
        MDC.clear();
    }

    public Object putMdc(final String key, final Object value) {
        try {
            return MDC.get(key);
        } finally {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, String.valueOf(value));
            }
        }
    }

    public Object getMdc(final String key) {
        return MDC.get(key);
    }

    public void removeMdc(final String key) {
        MDC.remove(key);
    }

    @SuppressWarnings({"unchecked"})
    public Map<String, Object> getMdcMap() {
        final Map<String, String> map = MDC.getCopyOfContextMap();
        return map == null ? Collections.emptyMap() : (Map) map;
    }
}
