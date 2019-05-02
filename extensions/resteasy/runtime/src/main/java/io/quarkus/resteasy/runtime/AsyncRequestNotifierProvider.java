package io.quarkus.resteasy.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.container.CompletionCallback;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.HttpRequest;

public class AsyncRequestNotifierProvider implements io.quarkus.arc.AsyncRequestNotifierProvider {

    @Override
    public CompletionStage<Void> getAsyncRequestNotifier() {
        HttpRequest resteasyHttpRequest = ResteasyContext.getContextData(HttpRequest.class);
        if (resteasyHttpRequest == null || !resteasyHttpRequest.getAsyncContext().isSuspended()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> ret = new CompletableFuture<>();
        resteasyHttpRequest.getAsyncContext().getAsyncResponse().register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                if (throwable != null)
                    ret.completeExceptionally(throwable);
                else
                    ret.complete(null);
            }
        });
        return ret;
    }

}
