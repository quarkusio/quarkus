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

import org.jboss.logmanager.MDC;
import org.jboss.logmanager.NDC;

import java.util.Map;

final class JBossLogManagerProvider implements LoggerProvider {

    public Logger getLogger(final String name) {
        return new JBossLogManagerLogger(name, org.jboss.logmanager.Logger.getLogger(name));
    }

    public void clearMdc() {
        MDC.clear();
    }

    public Object putMdc(final String key, final Object value) {
        return MDC.put(key, String.valueOf(value));
    }

    public Object getMdc(final String key) {
        return MDC.get(key);
    }

    public void removeMdc(final String key) {
        MDC.remove(key);
    }

    @SuppressWarnings({ "unchecked" })
    public Map<String, Object> getMdcMap() {
        // we can re-define the erasure of this map because MDC does not make further use of the copy
        return (Map)MDC.copy();
    }

    public void clearNdc() {
        NDC.clear();
    }

    public String getNdc() {
        return NDC.get();
    }

    public int getNdcDepth() {
        return NDC.getDepth();
    }

    public String popNdc() {
        return NDC.pop();
    }

    public String peekNdc() {
        return NDC.get();
    }

    public void pushNdc(final String message) {
        NDC.push(message);
    }

    public void setNdcMaxDepth(final int maxDepth) {
        NDC.trimTo(maxDepth);
    }
}
