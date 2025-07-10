/******************************************************************************
 * Copyright (c) 2016-2019 TypeFox and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 ******************************************************************************/
package io.quarkus.qute.debug.client;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageIssueException;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

/**
 * Outputs logs in a format that can be parsed by the LSP Inspector.
 * https://microsoft.github.io/language-server-protocol/inspector/
 * <p>
 * This class is a copy/paste of
 * https://github.com/eclipse-lsp4j/lsp4j/blob/main/org.eclipse.lsp4j.jsonrpc/src/main/java/org/eclipse/lsp4j/jsonrpc/TracingMessageConsumer.java
 * adapted for IJ.
 */
public class TracingMessageConsumer {
    private final Map<String, RequestMetadata> sentRequests;
    private final Map<String, RequestMetadata> receivedRequests;
    private final Clock clock;
    private final DateTimeFormatter dateTimeFormatter;

    public TracingMessageConsumer() {
        this.sentRequests = new ConcurrentHashMap<>();
        this.receivedRequests = new ConcurrentHashMap<>();
        this.clock = Clock.systemDefaultZone();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(clock.getZone());
    }

    /**
     * Constructs a log string for a given {@link Message}. The type of the {@link MessageConsumer}
     * determines if we're sending or receiving a message. The type of the @{link Message} determines
     * if it is a request, response, or notification.
     */
    public String log(Message message, MessageConsumer messageConsumer, ServerTrace serverTrace)
            throws MessageIssueException, JsonRpcException {
        final Instant now = clock.instant();
        final String date = dateTimeFormatter.format(now);

        if (messageConsumer instanceof StreamMessageConsumer) {
            return consumeMessageSending(message, now, date, serverTrace);
        } else if (messageConsumer instanceof RemoteEndpoint) {
            return consumeMessageReceiving(message, now, date, serverTrace);
        } else {
            return String.format("Unknown MessageConsumer type: %s", messageConsumer);
        }
    }

    private String consumeMessageSending(Message message, Instant now, String date, ServerTrace serverTrace) {
        if (message instanceof RequestMessage) {
            RequestMessage requestMessage = (RequestMessage) message;
            String id = requestMessage.getId();
            String method = requestMessage.getMethod();
            RequestMetadata requestMetadata = new RequestMetadata(method, now);
            sentRequests.put(id, requestMetadata);
            if (serverTrace == ServerTrace.messages) {
                String format = "[Trace - %s] Sending request '%s - (%s)'.\n";
                return String.format(format, date, method, id);
            }
            Object params = requestMessage.getParams();
            String paramsJson = MessageJsonHandler.toString(params);
            String format = "[Trace - %s] Sending request '%s - (%s)'.\nParams: %s\n\n\n";
            return String.format(format, date, method, id, paramsJson);
        } else if (message instanceof ResponseMessage) {
            ResponseMessage responseMessage = (ResponseMessage) message;
            String id = responseMessage.getId();
            RequestMetadata requestMetadata = receivedRequests.remove(id);
            String method = getMethod(requestMetadata);
            String latencyMillis = getLatencyMillis(requestMetadata, now);
            if (serverTrace == ServerTrace.messages) {
                String format = "[Trace - %s] Sending response '%s - (%s)'. Processing request took %sms\n";
                return String.format(format, date, method, id, latencyMillis);
            }
            Object result = responseMessage.getResult();
            String resultJson = MessageJsonHandler.toString(result);
            String resultTrace = getResultTrace(resultJson, null);
            String format = "[Trace - %s] Sending response '%s - (%s)'. Processing request took %sms\n%s\n\n\n";
            return String.format(format, date, method, id, latencyMillis, resultTrace);
        } else if (message instanceof NotificationMessage) {
            NotificationMessage notificationMessage = (NotificationMessage) message;
            String method = notificationMessage.getMethod();
            if (serverTrace == ServerTrace.messages) {
                String format = "[Trace - %s] Sending notification '%s'\n";
                return String.format(format, date, method);
            }
            Object params = notificationMessage.getParams();
            String paramsJson = MessageJsonHandler.toString(params);
            String format = "[Trace - %s] Sending notification '%s'\nParams: %s\n\n\n";
            return String.format(format, date, method, paramsJson);
        } else {
            return String.format("Unknown message type: %s", message);
        }
    }

    private String consumeMessageReceiving(Message message, Instant now, String date, ServerTrace serverTrace) {
        if (message instanceof RequestMessage) {
            RequestMessage requestMessage = (RequestMessage) message;
            String method = requestMessage.getMethod();
            String id = requestMessage.getId();
            RequestMetadata requestMetadata = new RequestMetadata(method, now);
            receivedRequests.put(id, requestMetadata);
            if (serverTrace == ServerTrace.messages) {
                String format = "[Trace - %s] Received request '%s - (%s)'.\n";
                return String.format(format, date, method, id);
            }
            Object params = requestMessage.getParams();
            String paramsJson = MessageJsonHandler.toString(params);
            String format = "[Trace - %s] Received request '%s - (%s)'\nParams: %s\n\n\n";
            return String.format(format, date, method, id, paramsJson);
        } else if (message instanceof ResponseMessage) {
            ResponseMessage responseMessage = (ResponseMessage) message;
            String id = responseMessage.getId();
            RequestMetadata requestMetadata = sentRequests.remove(id);
            String method = getMethod(requestMetadata);
            String latencyMillis = getLatencyMillis(requestMetadata, now);
            if (serverTrace == ServerTrace.messages) {
                String format = "[Trace - %s] Received response '%s - (%s)' in %sms.\n";
                return String.format(format, date, method, id, latencyMillis);
            }
            Object result = responseMessage.getResult();
            String resultJson = MessageJsonHandler.toString(result);
            Object error = responseMessage.getError();
            String errorJson = MessageJsonHandler.toString(error);
            String resultTrace = getResultTrace(resultJson, errorJson);
            String format = "[Trace - %s] Received response '%s - (%s)' in %sms.\n%s\n\n\n";
            return String.format(format, date, method, id, latencyMillis, resultTrace);
        } else if (message instanceof NotificationMessage) {
            NotificationMessage notificationMessage = (NotificationMessage) message;
            String method = notificationMessage.getMethod();
            if (serverTrace == ServerTrace.messages) {
                String format = "[Trace - %s] Received notification '%s'\n";
                return String.format(format, date, method);
            }
            Object params = notificationMessage.getParams();
            String paramsJson = MessageJsonHandler.toString(params);
            String format = "[Trace - %s] Received notification '%s'\nParams: %s\n\n\n";
            return String.format(format, date, method, paramsJson);
        } else {
            return String.format("Unknown message type: %s", message);
        }
    }

    private static String getResultTrace(String resultJson, String errorJson) {
        StringBuilder result = new StringBuilder();
        if (resultJson != null && !"null".equals(resultJson)) {
            result.append("Result: ");
            result.append(resultJson);
        } else {
            result.append("No response returned.");
        }
        if (errorJson != null && !"null".equals(errorJson)) {
            result.append("\nError: ");
            result.append(errorJson);
        }
        return result.toString();
    }

    private static String getMethod(RequestMetadata requestMetadata) {
        return requestMetadata != null ? requestMetadata.method : "<unknown>";
    }

    private static String getLatencyMillis(RequestMetadata requestMetadata, Instant now) {
        return requestMetadata != null ? String.valueOf(now.toEpochMilli() - requestMetadata.start.toEpochMilli()) : "?";
    }

    /**
     * Data class for holding pending request metadata.
     */
    public static class RequestMetadata {
        final String method;
        final Instant start;

        public RequestMetadata(String method, Instant start) {
            this.method = method;
            this.start = start;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RequestMetadata that = (RequestMetadata) o;
            return Objects.equals(method, that.method) && Objects.equals(start, that.start);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, start);
        }
    }
}
