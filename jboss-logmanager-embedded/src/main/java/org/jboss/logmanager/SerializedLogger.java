/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.logmanager;

import java.io.Serializable;

/**
 * A marker class for loggers.  After read, the {@link #readResolve()} method will return a logger with the given name.
 */
public final class SerializedLogger implements Serializable {

    private static final long serialVersionUID = 8266206989821750874L;

    private final String name;

    /**
     * Construct an instance.
     *
     * @param name the logger name
     */
    public SerializedLogger(final String name) {
        this.name = name;
    }

    /**
     * Get the actual logger for this marker.
     *
     * @return the logger
     * @see <a href="http://java.sun.com/javase/6/docs/platform/serialization/spec/input.html#5903">Serialization spec, 3.7</a>
     */
    public Object readResolve() {
        return java.util.logging.Logger.getLogger(name);
    }
}
