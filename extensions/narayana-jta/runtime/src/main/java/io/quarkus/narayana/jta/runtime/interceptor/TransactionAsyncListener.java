package io.quarkus.narayana.jta.runtime.interceptor;

import java.io.IOException;
import java.util.function.Consumer;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.ws.rs.container.CompletionCallback;

public class TransactionAsyncListener implements AsyncListener, CompletionCallback {

    private Throwable throwable;
    private Runnable endListener;
    private Consumer<Throwable> exceptionHandler;

    public TransactionAsyncListener(Runnable endListener, Consumer<Throwable> exceptionHandler) {
        this.endListener = endListener;
        this.exceptionHandler = exceptionHandler;
    }

    // JAX-RS
    @Override
    public void onComplete(Throwable throwable) {
        this.throwable = throwable;
    }

    // Servlet

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        if (throwable != null)
            exceptionHandler.accept(throwable);
        endListener.run();
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        // FIXME: should this rollback?
        onComplete(event);
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        this.throwable = event.getThrowable();
        onComplete(event);
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
        // not interested
    }

}
