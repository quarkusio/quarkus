package io.quarkus.undertow.runtime;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import io.undertow.servlet.handlers.ServletRequestContext;

public class AsyncRequestNotifierProvider implements io.quarkus.arc.AsyncRequestNotifierProvider {

    @Override
    public CompletionStage<Void> getAsyncRequestNotifier() {
        ServletRequestContext req = ServletRequestContext.current();
        if (req == null)
            return CompletableFuture.completedFuture(null);
        CompletableFuture<Void> ret = new CompletableFuture<>();
        req.getServletRequest().getAsyncContext().addListener(new AsyncListener() {

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                ret.completeExceptionally(new TimeoutException());
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                ret.completeExceptionally(event.getThrowable());
            }

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                ret.complete(null);
            }
        });
        return ret;
    }

}
