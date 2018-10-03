/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.util.logging.Handler;

/**
 * The configurator for the embedded log manager.
 */
public interface EmbeddedConfigurator {
    Handler[] NO_HANDLERS = new Handler[0];

    EmbeddedConfigurator EMPTY = new EmbeddedConfigurator() {
    };

    /**
     * Get the configured minimum level of the given logger name.
     *
     * @param loggerName the logger name (not {@code null})
     * @return the minimum level of the given logger name, or {@code null} to inherit
     */
    default java.util.logging.Level getMinimumLevelOf(String loggerName) {
        return null;
    }

    /**
     * Get the configured level of the given logger name.
     *
     * @param loggerName the logger name (not {@code null})
     * @return the level of the given logger name, or {@code null} to inherit
     */
    default java.util.logging.Level getLevelOf(String loggerName) {
        return null;
    }

    /**
     * Get the handlers of the given logger name.
     *
     * @param loggerName the logger name (not {@code null})
     * @return the handlers of the given logger name (must not be {@code null})
     */
    default Handler[] getHandlersOf(String loggerName) {
        return NO_HANDLERS;
    }
}
