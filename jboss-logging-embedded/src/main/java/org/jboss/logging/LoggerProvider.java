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
 * A contract for the log provider implementation.
 */
public interface LoggerProvider {
    /**
     * Returns a logger which is backed by a logger from the log provider.
     *
     * <p>
     * <b>Note:</b> this should never be {@code null}
     * </p>
     *
     * @param name the name of the logger
     *
     * @return a logger for the log provider logger.
     */
    Logger getLogger(String name);

    /**
     * Removes all entries from the message diagnostics context.
     */
    void clearMdc();

    /**
     * Puts the value onto the message diagnostics context.
     *
     * @param key   the key for the value
     * @param value the value
     *
     * @return the previous value set or {@code null} if no value was set
     */
    Object putMdc(String key, Object value);

    /**
     * Returns the value for the key on the message diagnostics context or {@code null} if no value was found.
     *
     * @param key the key to lookup the value for
     *
     * @return the value or {@code null} if not found
     */
    Object getMdc(String key);

    /**
     * Removes the value from the message diagnostics context.
     *
     * @param key the key of the value to remove
     */
    void removeMdc(String key);

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
    Map<String, Object> getMdcMap();

    /**
     * Clears the nested diagnostics context.
     */
    void clearNdc();

    /**
     * Retrieves the current values set for the nested diagnostics context.
     *
     * @return the current value set or {@code null} if no value was set
     */
    String getNdc();

    /**
     * The current depth of the nested diagnostics context.
     *
     * @return the current depth of the stack
     */
    int getNdcDepth();

    /**
     * Pops top value from the stack and returns it.
     *
     * @return the top value from the stack or an empty string if no value was set
     */
    String popNdc();

    /**
     * Peeks at the top value from the stack and returns it.
     *
     * @return the value or an empty string
     */
    String peekNdc();

    /**
     * Pushes a value to the nested diagnostics context stack.
     *
     * @param message the message to push
     */
    void pushNdc(String message);

    /**
     * Sets maximum depth of the stack removing any entries below the maximum depth.
     *
     * @param maxDepth the maximum depth to set
     */
    void setNdcMaxDepth(int maxDepth);
}
