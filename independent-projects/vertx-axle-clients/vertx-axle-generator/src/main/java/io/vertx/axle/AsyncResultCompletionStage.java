package io.vertx.axle;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class AsyncResultCompletionStage {

  // Simplest implementation for now
  public static <T> CompletionStage<T> toCompletionStage(Consumer<Handler<AsyncResult<T>>> completionConsumer) {
    CompletableFuture<T> cs = new CompletableFuture<>();
    try {
      completionConsumer.accept(ar -> {
        if (ar.succeeded()) {
          cs.complete(ar.result());
        } else {
          cs.completeExceptionally(ar.cause());
        }
      });
    } catch (Exception e) {
      // unsure we need this ?
      cs.completeExceptionally(e);
    }
    return cs;
  }
}
