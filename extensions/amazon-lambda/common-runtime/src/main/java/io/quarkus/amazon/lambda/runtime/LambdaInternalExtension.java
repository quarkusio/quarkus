package io.quarkus.amazon.lambda.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Registers a lightweight internal Lambda extension and keeps a dedicated thread in the Extensions API event loop.
 * <p>
 * Lambda can freeze the execution environment shortly after the function runtime reports an invoke response. Quarkus
 * may still have short post-response work (for example telemetry export). Even with {@code quarkus.otel.simple=true},
 * this may not cover every post-response case. For that reason, it is safer for the Quarkus runtime thread to signal
 * an internal extension thread when invocation work is done, via {@link #invocationFinished(String)} keyed by request
 * id.
 * <p>
 * The extension registers only for {@code INVOKE} events and then blocks on {@code /extension/event/next}. For each
 * invoke event, it waits until the corresponding runtime request id is marked complete and then returns to the next
 * event. AWS documentation for the Extensions API lifecycle and timing (including the documented 500 ms SHUTDOWN
 * phase budget for internal extensions) is here:
 * https://docs.aws.amazon.com/lambda/latest/dg/runtimes-extensions-api.html
 */
public final class LambdaInternalExtension {
    private static final Logger log = Logger.getLogger(LambdaInternalExtension.class);

    private static final String API_PROTOCOL = "http://";
    private static final String API_PATH_EXTENSION = "/2020-01-01/extension/";
    private static final String API_PATH_EXTENSION_REGISTER = API_PATH_EXTENSION + "register";
    private static final String API_PATH_EXTENSION_NEXT = API_PATH_EXTENSION + "event/next";
    private static final String HEADER_EXTENSION_NAME = "Lambda-Extension-Name";
    private static final String HEADER_EXTENSION_ID = "Lambda-Extension-Identifier";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String USER_AGENT = "User-Agent";
    private static final String USER_AGENT_VALUE = String.format(
            "quarkus/%s-%s",
            System.getProperty("java.vendor.version"),
            LambdaInternalExtension.class.getPackage().getImplementationVersion());
    private static final String CONFIG_ENABLED = "quarkus.lambda.internal-extension.enabled";
    private static final String CONFIG_NAME = "quarkus.lambda.internal-extension.name";
    private static final int REGISTER_CONNECT_TIMEOUT_MILLIS = 1_000;
    private static final int REGISTER_READ_TIMEOUT_MILLIS = 1_000;
    private static final long INVOCATION_WAIT_FALLBACK_MILLIS = 30_000L;
    private static final long INVOCATION_POST_COMPLETION_DELAY_MILLIS = 500L;
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final AtomicBoolean started = new AtomicBoolean(false);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final ConcurrentHashMap<String, CompletableFuture<Void>> invocationCompletion = new ConcurrentHashMap<>();
    private static volatile Thread eventThread;

    private LambdaInternalExtension() {
    }

    public static void startIfEnabled() {
        boolean enabled = ConfigProvider.getConfig().getOptionalValue(CONFIG_ENABLED, Boolean.class).orElse(false);
        if (!enabled) {
            log.info("Lambda internal extension not active");
            return;
        }

        String runtimeApi = System.getenv("AWS_LAMBDA_RUNTIME_API");
        if (runtimeApi == null || runtimeApi.isBlank()) {
            log.info("Lambda internal extension is enabled but AWS_LAMBDA_RUNTIME_API is not set, skipping startup");
            return;
        }
        if (!started.compareAndSet(false, true)) {
            log.info("Lambda internal extension already started");
            return;
        }

        String extensionName = ConfigProvider.getConfig().getOptionalValue(CONFIG_NAME, String.class)
                .orElse("quarkus-internal-extension");
        log.infof("Starting Lambda internal extension '%s' against runtime API '%s'", extensionName, runtimeApi);

        String extensionId = register(runtimeApi, extensionName);
        if (extensionId == null || extensionId.isBlank()) {
            started.set(false);
            running.set(false);
            log.infof("Lambda internal extension '%s' could not be registered", extensionName);
            return;
        }

        running.set(true);
        eventThread = new Thread(() -> runLoop(runtimeApi, extensionName, extensionId), "Quarkus Lambda Internal Extension");
        eventThread.setDaemon(true);
        eventThread.start();
    }

    public static void stop() {
        running.set(false);
        log.info("Stopping Lambda internal extension");
        invocationCompletion.clear();
        Thread localThread = eventThread;
        if (localThread != null) {
            localThread.interrupt();
        }
    }

    public static void invocationFinished(String requestId) {
        if (!started.get()) {
            return;
        }
        if (requestId == null || requestId.isBlank()) {
            return;
        }
        invocationCompletion.computeIfAbsent(requestId, ignored -> new CompletableFuture<>()).complete(null);
    }

    private static void runLoop(String runtimeApi, String extensionName, String extensionId) {
        log.infof("Lambda internal extension '%s' event loop started", extensionName);
        try {
            log.infof("Lambda internal extension '%s' entering event/next loop", extensionName);
            URL nextUrl;
            try {
                nextUrl = new URL(API_PROTOCOL + runtimeApi + API_PATH_EXTENSION_NEXT);
            } catch (IOException e) {
                log.warnf(e, "Lambda internal extension '%s' cannot create event/next URL", extensionName);
                return;
            }
            while (running.get()) {
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) nextUrl.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
                    conn.setRequestProperty(HEADER_EXTENSION_ID, extensionId);
                    long nextStartNanos = System.nanoTime();
                    log.debugf("Lambda internal extension '%s' waiting for next event at %s", extensionName, nextUrl);
                    int nextCode = conn.getResponseCode();
                    long nextDurationMillis = millisSince(nextStartNanos);
                    if (nextCode != 200) {
                        log.warnf("Lambda internal extension '%s' next call returned %d after %d ms",
                                extensionName, nextCode, nextDurationMillis);
                        log.warnf("Lambda internal extension '%s' next call error body: %s",
                                extensionName, readErrorBody(conn));
                        return;
                    }
                    InvokeEvent invokeEvent = parseInvokeEvent(conn);
                    if (log.isDebugEnabled()) {
                        log.debugf("Lambda internal extension '%s' received next event after %d ms (requestId=%s)",
                                extensionName, nextDurationMillis, invokeEvent != null ? invokeEvent.requestId() : "n/a");
                    }
                    if (invokeEvent != null) {
                        awaitInvocationCompletion(extensionName, invokeEvent);
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        log.warnf(e, "Lambda internal extension '%s' event loop ended unexpectedly", extensionName);
                    }
                    return;
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        } finally {
            started.set(false);
            running.set(false);
            invocationCompletion.clear();
            eventThread = null;
            log.infof("Lambda internal extension '%s' event loop stopped", extensionName);
        }
    }

    private static String register(String runtimeApi, String extensionName) {
        HttpURLConnection conn = null;
        try {
            URL registerUrl = new URL(API_PROTOCOL + runtimeApi + API_PATH_EXTENSION_REGISTER);
            conn = (HttpURLConnection) registerUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
            conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
            conn.setRequestProperty(HEADER_EXTENSION_NAME, extensionName);
            conn.setConnectTimeout(REGISTER_CONNECT_TIMEOUT_MILLIS);
            conn.setReadTimeout(REGISTER_READ_TIMEOUT_MILLIS);

            // Internal extensions are not allowed to register SHUTDOWN events.
            byte[] payload = "{\"events\":[\"INVOKE\"]}".getBytes(StandardCharsets.UTF_8);
            log.infof("Registering Lambda internal extension '%s' at %s with payload %s",
                    extensionName, registerUrl, new String(payload, StandardCharsets.UTF_8));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            long registerStartNanos = System.nanoTime();
            log.infof("Waiting for register response for Lambda internal extension '%s'", extensionName);
            int code = conn.getResponseCode();
            long registerDurationMillis = millisSince(registerStartNanos);
            log.infof("Register response for Lambda internal extension '%s': status=%d after %d ms",
                    extensionName, code, registerDurationMillis);
            if (code != 200) {
                log.warnf("Could not register internal extension '%s'. status=%d body=%s",
                        extensionName, code, readErrorBody(conn));
                return null;
            }

            String extensionId = conn.getHeaderField(HEADER_EXTENSION_ID);
            if (extensionId == null || extensionId.isBlank()) {
                log.warnf("Internal extension '%s' registered but no extension identifier was returned", extensionName);
            } else {
                log.infof("Lambda internal extension '%s' registered successfully", extensionName);
            }
            return extensionId;
        } catch (IOException e) {
            log.warnf(e, "Could not register internal extension '%s'", extensionName);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static InvokeEvent parseInvokeEvent(HttpURLConnection conn) {
        try (var is = conn.getInputStream()) {
            JsonNode event = JSON.readTree(is);
            if (!"INVOKE".equals(event.path("eventType").asText())) {
                return null;
            }
            String requestId = event.path("requestId").asText(null);
            if (requestId == null || requestId.isBlank()) {
                return null;
            }
            Long deadlineMs = event.hasNonNull("deadlineMs") ? event.path("deadlineMs").asLong() : null;
            return new InvokeEvent(requestId, deadlineMs);
        } catch (Exception e) {
            log.warnf(e, "Could not parse extension event body");
            return null;
        }
    }

    private static void awaitInvocationCompletion(String extensionName, InvokeEvent invokeEvent) {
        String requestId = invokeEvent.requestId();
        CompletableFuture<Void> completion = invocationCompletion.computeIfAbsent(requestId,
                ignored -> new CompletableFuture<>());
        long maxWaitMillis = waitMillisUntil(invokeEvent.deadlineMs());
        boolean completionReceived = false;
        log.debugf("Lambda internal extension '%s' waiting for invocation completion requestId=%s (max %d ms)",
                extensionName, requestId, maxWaitMillis);
        try {
            completion.get(maxWaitMillis, TimeUnit.MILLISECONDS);
            completionReceived = true;
            log.debugf("Lambda internal extension '%s' invocation completed requestId=%s", extensionName, requestId);
        } catch (TimeoutException e) {
            log.warnf("Lambda internal extension '%s' timed out waiting for invocation completion requestId=%s after %d ms",
                    extensionName, requestId, maxWaitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warnf("Lambda internal extension '%s' interrupted while waiting for requestId=%s", extensionName, requestId);
        } catch (ExecutionException e) {
            log.warnf(e, "Lambda internal extension '%s' failed while waiting for requestId=%s", extensionName, requestId);
        } finally {
            invocationCompletion.remove(requestId, completion);
        }

        if (completionReceived && running.get()) {
            sleepPostCompletionDelay(extensionName, requestId);
        }
    }

    private static void sleepPostCompletionDelay(String extensionName, String requestId) {
        try {
            log.debugf("Lambda internal extension '%s' sleeping %d ms after completion requestId=%s",
                    extensionName, INVOCATION_POST_COMPLETION_DELAY_MILLIS, requestId);
            Thread.sleep(INVOCATION_POST_COMPLETION_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warnf("Lambda internal extension '%s' interrupted during post-completion delay requestId=%s",
                    extensionName, requestId);
        }
    }

    private static long waitMillisUntil(Long deadlineMs) {
        if (deadlineMs == null) {
            return INVOCATION_WAIT_FALLBACK_MILLIS;
        }
        long remaining = deadlineMs - System.currentTimeMillis();
        return Math.max(1L, remaining);
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static String readErrorBody(HttpURLConnection conn) {
        var errorStream = conn.getErrorStream();
        if (errorStream == null) {
            return "";
        }
        try {
            try (var is = errorStream; var baos = new ByteArrayOutputStream()) {
                is.transferTo(baos);
                return baos.toString(StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private record InvokeEvent(String requestId, Long deadlineMs) {
    }
}
