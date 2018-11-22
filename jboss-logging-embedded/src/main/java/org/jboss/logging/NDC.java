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

public final class NDC {

    private NDC() {
    }

    /**
     * Clears the nested diagnostics context.
     */
    public static void clear() {
        LoggerProviders.PROVIDER.clearNdc();
    }

    /**
     * Retrieves the current values set for the nested diagnostics context.
     *
     * @return the current value set or {@code null} if no value was set
     */
    public static String get() {
        return LoggerProviders.PROVIDER.getNdc();
    }

    /**
     * The current depth of the nested diagnostics context.
     *
     * @return the current depth of the stack
     */
    public static int getDepth() {
        return LoggerProviders.PROVIDER.getNdcDepth();
    }

    /**
     * Pops top value from the stack and returns it.
     *
     * @return the top value from the stack or an empty string if no value was set
     */
    public static String pop() {
        return LoggerProviders.PROVIDER.popNdc();
    }

    /**
     * Peeks at the top value from the stack and returns it.
     *
     * @return the value or an empty string
     */
    public static String peek() {
        return LoggerProviders.PROVIDER.peekNdc();
    }

    /**
     * Pushes a value to the nested diagnostics context stack.
     *
     * @param message the message to push
     */
    public static void push(String message) {
        LoggerProviders.PROVIDER.pushNdc(message);
    }

    /**
     * Sets maximum depth of the stack removing any entries below the maximum depth.
     *
     * @param maxDepth the maximum depth to set
     */
    public static void setMaxDepth(int maxDepth) {
        LoggerProviders.PROVIDER.setNdcMaxDepth(maxDepth);
    }
}
