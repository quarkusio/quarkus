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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;

final class Log4j2LoggerProvider implements LoggerProvider {

    @Override
    public Log4j2Logger getLogger(String name) {
        return new Log4j2Logger(name);
    }

    @Override
    public void clearMdc() {
        ThreadContext.clearMap();
    }

    @Override
    public Object putMdc(String key, Object value) {
        try {
            return ThreadContext.get(key);
        } finally {
            ThreadContext.put(key, String.valueOf(value));
        }
    }

    @Override
    public Object getMdc(String key) {
        return ThreadContext.get(key);
    }

    @Override
    public void removeMdc(String key) {
        ThreadContext.remove(key);
    }

    @Override
    public Map<String, Object> getMdcMap() {
        return new HashMap<String, Object>(ThreadContext.getImmutableContext());
    }

    @Override
    public void clearNdc() {
        ThreadContext.clearStack();
    }

    @Override
    public String getNdc() {
        return ThreadContext.peek();
    }

    @Override
    public int getNdcDepth() {
        return ThreadContext.getDepth();
    }

    @Override
    public String popNdc() {
        return ThreadContext.pop();
    }

    @Override
    public String peekNdc() {
        return ThreadContext.peek();
    }

    @Override
    public void pushNdc(String message) {
        ThreadContext.push(message);
    }

    @Override
    public void setNdcMaxDepth(int maxDepth) {
        ThreadContext.trim(maxDepth);
    }
}
