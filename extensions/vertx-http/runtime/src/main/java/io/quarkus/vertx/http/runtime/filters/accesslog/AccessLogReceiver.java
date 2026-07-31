/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.quarkus.vertx.http.runtime.filters.accesslog;

/**
 * Interface that is used by the access log handler to send data to the log file manager.
 *
 * Implementations of this interface must be thread safe.
 *
 * @author Stuart Douglas
 */
public interface AccessLogReceiver {

    void logMessage(final String message);

    /**
     * Log a message with the originating {@link io.vertx.ext.web.RoutingContext} so that
     * implementations can extract structured fields (method, URI, status code, etc.) from
     * the exchange and attach them to the log record.
     * <p>
     * Implementations that do not need structured access to the exchange may rely on the
     * default implementation which simply delegates to {@link #logMessage(String)}.
     *
     * @param message the pre-formatted access log line
     * @param exchange the Vert.x routing context for the completed request
     */
    default void logMessage(final String message, final io.vertx.ext.web.RoutingContext exchange) {
        logMessage(message);
    }
}
