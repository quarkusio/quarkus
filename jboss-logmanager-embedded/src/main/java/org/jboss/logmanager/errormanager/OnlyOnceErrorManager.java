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

package org.jboss.logmanager.errormanager;

import java.util.concurrent.atomic.AtomicBoolean;

import java.util.logging.ErrorManager;

import org.jboss.logmanager.StandardOutputStreams;

/**
 * An error manager which runs only once and writes a complete formatted error to {@code System.err}.  Caches
 * an early {@code System.err} in case it is replaced.
 */
public final class OnlyOnceErrorManager extends ErrorManager {

    private final AtomicBoolean called = new AtomicBoolean();

    /** {@inheritDoc} */
    public void error(final String msg, final Exception ex, final int code) {
        if (called.getAndSet(true)) {
            return;
        }
        final String codeStr;
        switch (code) {
            case CLOSE_FAILURE: codeStr = "CLOSE_FAILURE"; break;
            case FLUSH_FAILURE: codeStr = "FLUSH_FAILURE"; break;
            case FORMAT_FAILURE: codeStr = "FORMAT_FAILURE"; break;
            case GENERIC_FAILURE: codeStr = "GENERIC_FAILURE"; break;
            case OPEN_FAILURE: codeStr = "OPEN_FAILURE"; break;
            case WRITE_FAILURE: codeStr = "WRITE_FAILURE"; break;
            default: codeStr = "INVALID (" + code + ")"; break;
        }
        StandardOutputStreams.printError(ex, "LogManager error of type %s: %s%n", codeStr, msg);
    }
}
