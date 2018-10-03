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

package org.jboss.logmanager.handlers;

import java.util.logging.Handler;

/**
 * Handler utility methods.
 */
public final class Handlers {

    private Handlers() {
    }

    /**
     * Create a wrapper that exposes the handler's close and flush methods via the I/O API.
     *
     * @param handler the logging handler
     * @return the wrapper
     */
    public static FlushableCloseable wrap(final Handler handler) {
        return handler instanceof FlushableCloseable ? (FlushableCloseable) handler : new FlushableCloseable() {
            public void close() {
                handler.close();
            }

            public void flush() {
                handler.flush();
            }
        };
    }

    /**
     * Create a {@code Runnable} task that flushes a handler.
     *
     * @param handler the handler
     * @return a flushing task
     */
    public static Runnable flusher(final Handler handler) {
        return new Runnable() {
            public void run() {
                handler.flush();
            }
        };
    }
}
