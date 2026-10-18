package io.quarkus.dev.testing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class GrpcWebSocketProxy {

    private static final AtomicInteger connectionIdSeq = new AtomicInteger();

    private static volatile WebSocketListener webSocketListener;

    private static final Map<Integer, Consumer<Runnable>> webSocketConnections = new ConcurrentHashMap<>();

    public static Integer addWebSocket(Consumer<String> responseConsumer,
            Consumer<Runnable> closeHandler) {
        if (webSocketListener != null) {
            int id = connectionIdSeq.getAndIncrement();
            webSocketListener.onOpen(id, responseConsumer);

            webSocketConnections.put(id, closeHandler);
            return id;
        }
        return null;
    }

    public static void closeAll() {
        CountDownLatch latch = new CountDownLatch(webSocketConnections.size());
        for (Map.Entry<Integer, Consumer<Runnable>> connection : webSocketConnections.entrySet()) {
            connection.getValue().accept(latch::countDown);
            webSocketListener.onClose(connection.getKey());
        }
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                System.err.println("Failed to close all the websockets in 5 seconds");
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for websockets to be closed");
        }
    }

    public static void closeWebSocket(int id) {
        webSocketListener.onClose(id);
    }

    public static void setWebSocketListener(WebSocketListener listener) {
        webSocketListener = listener;
    }

    public static void addMessage(Integer socketId, String message) {
        webSocketListener.newMessage(socketId, message);
    }

    public interface WebSocketListener {
        void onOpen(int id, Consumer<String> responseConsumer);

        void newMessage(int id, String content);

        void onClose(int id);
    }
}
