package io.quarkus.aesh.websocket.deployment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import io.vertx.core.http.WebSocket;

/**
 * Helper for aesh WebSocket tests that handles prompt detection reliably.
 * <p>
 * Terminal output arrives as fragmented WebSocket messages. The prompt
 * characters may be split across multiple messages or embedded in ANSI
 * escape sequences. This helper accumulates all output in a thread-safe
 * buffer and checks the full buffer for the prompt.
 */
final class AeshWebSocketTestHelper {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[\\d;]*[a-zA-Z@-~]|\\u001B[()][A-Z0-9]|\\r");

    private AeshWebSocketTestHelper() {
    }

    /**
     * Sets up a text message handler on the WebSocket that:
     * <ol>
     * <li>Accumulates all output in {@code outputBuffer}</li>
     * <li>Sends the command once the prompt is detected in the accumulated output</li>
     * <li>Counts down the latch when {@code expectedOutput} appears</li>
     * </ol>
     */
    static void sendCommandOnPrompt(WebSocket ws, String command, String expectedOutput,
            StringBuilder outputBuffer, CountDownLatch latch) {
        AtomicBoolean commandSent = new AtomicBoolean(false);
        AtomicBoolean resultFound = new AtomicBoolean(false);

        ws.textMessageHandler(msg -> {
            outputBuffer.append(msg);
            String stripped = ANSI_PATTERN.matcher(outputBuffer.toString()).replaceAll("");

            if (!commandSent.get() && stripped.contains("$ ")) {
                if (commandSent.compareAndSet(false, true)) {
                    ws.writeTextMessage("{\"action\":\"read\",\"data\":\"" + command + "\\r\"}");
                }
            }

            if (!resultFound.get() && stripped.contains(expectedOutput)) {
                if (resultFound.compareAndSet(false, true)) {
                    latch.countDown();
                }
            }
        });
    }
}
