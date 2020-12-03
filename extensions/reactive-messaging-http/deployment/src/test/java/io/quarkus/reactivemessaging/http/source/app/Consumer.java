package io.quarkus.reactivemessaging.http.source.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.reactivemessaging.utils.VertxFriendlyLock;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class Consumer {
    private static final CompletableFuture<Void> COMPLETED;

    static {
        COMPLETED = new CompletableFuture<>();
        COMPLETED.complete(null);
    }

    private final List<Message<?>> postMessages = new ArrayList<>();
    private final List<Message<?>> putMessages = new ArrayList<>();
    private final List<Object> payloads = new ArrayList<>();

    VertxFriendlyLock lock;

    @Inject
    Consumer(Vertx vertx) {
        lock = new VertxFriendlyLock(vertx);
    }

    @Incoming("post-http-source")
    public CompletionStage<Void> process(Message<?> message) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        lock.triggerWhenUnlocked(() -> {
            postMessages.add(message);
            message.ack();
            result.complete(null);
        }, 10000);
        return result;
    }

    @Incoming("put-http-source")
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public CompletionStage<Void> processPut(Message<?> message) {
        putMessages.add(message);
        return COMPLETED;
    }

    @Incoming("json-http-source")
    public CompletionStage<Void> processJsonObject(JsonObject jsonObject) {
        payloads.add(jsonObject);
        return COMPLETED;
    }

    @Incoming("jsonarray-http-source")
    public CompletionStage<Void> processJsonArray(JsonArray jsonArray) {
        payloads.add(jsonArray);
        return COMPLETED;
    }

    @Incoming("string-http-source")
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public CompletionStage<Void> processString(Message<String> stringMessage) {
        payloads.add(stringMessage.getPayload());
        return COMPLETED;
    }

    public List<Message<?>> getPostMessages() {
        return postMessages;
    }

    public List<Message<?>> getPutMessages() {
        return putMessages;
    }

    public List<?> getPayloads() {
        return payloads;
    }

    public void pause() {
        lock.lock();
    }

    public void resume() {
        lock.unlock();
    }

    public void clear() {
        postMessages.clear();
        putMessages.clear();
        payloads.clear();
        lock.reset();
    }
}
