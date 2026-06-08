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

import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import io.quarkus.vertx.http.runtime.attribute.BytesSentAttribute;
import io.quarkus.vertx.http.runtime.attribute.RemoteIPAttribute;
import io.quarkus.vertx.http.runtime.attribute.RequestMethodAttribute;
import io.quarkus.vertx.http.runtime.attribute.RequestProtocolAttribute;
import io.quarkus.vertx.http.runtime.attribute.RequestURLAttribute;
import io.quarkus.vertx.http.runtime.attribute.ResponseCodeAttribute;
import io.quarkus.vertx.http.runtime.attribute.ResponseTimeAttribute;
import io.vertx.ext.web.RoutingContext;

/**
 * Access log receiver that logs messages at INFO level via JBoss Logging.
 * <p>
 * When {@link #logMessage(String, RoutingContext)} is called, structured request
 * fields are injected into the thread-local MDC under reserved keys
 * (all prefixed with {@value #MDC_FIELD_PREFIX}) so that log formatters such as
 * {@code quarkus-logging-json} can emit them as first-class JSON fields.
 * The MDC entries are removed in a {@code finally} block, so they never leak
 * beyond the log call.
 *
 * @author Stuart Douglas
 */
public class JBossLoggingAccessLogReceiver implements AccessLogReceiver {

    /**
     * Prefix shared by every MDC key written by this receiver.
     * Consumers (e.g. logging-json) can use this prefix to discover
     * and process structured access-log fields.
     */
    public static final String MDC_FIELD_PREFIX = "__access__";

    /** MDC key for the HTTP request method (e.g. {@code GET}). */
    public static final String MDC_METHOD = MDC_FIELD_PREFIX + "method";
    /** MDC key for the request URI, including the query string when present. */
    public static final String MDC_URI = MDC_FIELD_PREFIX + "uri";
    /** MDC key for the HTTP response status code (e.g. {@code 200}). */
    public static final String MDC_STATUS = MDC_FIELD_PREFIX + "status";
    /** MDC key for the response time in milliseconds. */
    public static final String MDC_RESPONSE_TIME_MS = MDC_FIELD_PREFIX + "responseTimeMs";
    /** MDC key for the number of bytes sent (excluding HTTP headers). */
    public static final String MDC_BYTES_SENT = MDC_FIELD_PREFIX + "bytesSent";
    /** MDC key for the remote IP address of the client. */
    public static final String MDC_REMOTE_IP = MDC_FIELD_PREFIX + "remoteIp";
    /** MDC key for the HTTP protocol version (e.g. {@code HTTP/1.1}). */
    public static final String MDC_PROTOCOL = MDC_FIELD_PREFIX + "protocol";

    private static final ResponseTimeAttribute RESPONSE_TIME_ATTR = new ResponseTimeAttribute(TimeUnit.MILLISECONDS);
    private static final BytesSentAttribute BYTES_SENT_ATTR = new BytesSentAttribute(false);

    public static final String DEFAULT_CATEGORY = "io.quarkus.vertx.http.accesslog";

    private final Logger logger;

    public JBossLoggingAccessLogReceiver(final String category) {
        this.logger = Logger.getLogger(category);
    }

    public JBossLoggingAccessLogReceiver() {
        this.logger = Logger.getLogger(DEFAULT_CATEGORY);
    }

    @Override
    public void logMessage(String message) {
        logger.info(message);
    }

    /**
     * Log the access-log line and populate the thread-local MDC with structured
     * request/response fields for the duration of the logging call.
     */
    @Override
    public void logMessage(String message, RoutingContext exchange) {
        putMdcField(MDC_METHOD, RequestMethodAttribute.INSTANCE.readAttribute(exchange));
        putMdcField(MDC_URI, RequestURLAttribute.INSTANCE.readAttribute(exchange));
        putMdcField(MDC_STATUS, ResponseCodeAttribute.INSTANCE.readAttribute(exchange));
        putMdcField(MDC_RESPONSE_TIME_MS, RESPONSE_TIME_ATTR.readAttribute(exchange));
        putMdcField(MDC_BYTES_SENT, BYTES_SENT_ATTR.readAttribute(exchange));
        putMdcField(MDC_REMOTE_IP, RemoteIPAttribute.INSTANCE.readAttribute(exchange));
        putMdcField(MDC_PROTOCOL, RequestProtocolAttribute.INSTANCE.readAttribute(exchange));
        try {
            logger.info(message);
        } finally {
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_URI);
            MDC.remove(MDC_STATUS);
            MDC.remove(MDC_RESPONSE_TIME_MS);
            MDC.remove(MDC_BYTES_SENT);
            MDC.remove(MDC_REMOTE_IP);
            MDC.remove(MDC_PROTOCOL);
        }
    }

    private static void putMdcField(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
}
