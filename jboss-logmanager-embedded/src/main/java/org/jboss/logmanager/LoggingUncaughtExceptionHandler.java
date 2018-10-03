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

/**
 * Logging uncaught exception handler.
 */
public final class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Logger log;

    /**
     * Create a new instance.
     *
     * @param log the logger to log the uncaught exception to
     */
    public LoggingUncaughtExceptionHandler(final Logger log) {
        this.log = log;
    }

    /**
     * Method invoked when the given thread terminates due to the given uncaught exception. <p>Any exception thrown by this
     * method will be ignored by the Java Virtual Machine.
     *
     * @param t the thread
     * @param e the exception
     */
    public void uncaughtException(final Thread t, final Throwable e) {
        log.log(Level.ERROR, "Uncaught exception", e);
    }
}
