package io.quarkus.resteasy.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

import io.vertx.core.impl.VertxThread;

/**
 * Output stream that will resort to buffering + async writes if invoked from an IO thread.
 *
 * This allows for reactive JAX-RS to work correctly, as otherwise writes from the IO thread
 * will fail as Undertow does not allow for blocking writes from an IO thread.
 */
public class ResteasyAsyncOutputStream extends ServletOutputStream {

    private static final Logger log = Logger.getLogger(ResteasyAsyncOutputStream.class);

    private ServletOutputStream delegate;
    private ByteArrayOutputStream buffered;
    private final HttpServletResponse response;
    private final HttpServletRequest request;

    public ResteasyAsyncOutputStream(HttpServletResponse response, HttpServletRequest request) {
        this.response = response;
        this.request = request;
    }

    boolean inIoThread() {
        //note that we can't just use the inEventLoop() call
        //as we don't want to block any vertx thread, not just our current one
        //although really we should be trying to make all async stuff happen on our IO thread
        return Thread.currentThread() instanceof VertxThread;
    }

    @Override
    public boolean isReady() {
        if (delegate == null) {
            return false;
        }
        return delegate.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        //generally this should never happen, but its possible a user might be doing stuff
        //with custom filters, so try and handle it as best we can
        if (delegate == null) {
            try {
                delegate = response.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        delegate.setWriteListener(writeListener);
    }

    @Override
    public void write(int b) throws IOException {
        checkAsyncStart();
        if (buffered != null) {
            buffered.write(b);
            return;
        }
        if (delegate == null) {
            delegate = response.getOutputStream();
        }
        delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkAsyncStart();
        if (buffered != null) {
            buffered.write(b);
            return;
        }
        if (delegate == null) {
            delegate = response.getOutputStream();
        }
        delegate.write(b);

    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkAsyncStart();
        if (buffered != null) {
            buffered.write(b, off, len);
            return;
        }
        if (delegate == null) {
            delegate = response.getOutputStream();
        }
        delegate.write(b, off, len);

    }

    private void checkAsyncStart() {
        if (buffered == null && inIoThread()) {
            buffered = new ByteArrayOutputStream();
            if (!request.isAsyncStarted()) {
                request.startAsync();
            }
            request.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                    request.startAsync();
                    close();
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    request.startAsync();
                    close();
                }

                @Override
                public void onError(AsyncEvent event) throws IOException {
                    request.startAsync();
                    close();
                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {

                }
            });
        }
    }

    @Override
    public void flush() throws IOException {
        if (buffered == null) {
            if (delegate == null) {
                delegate = response.getOutputStream();
            }
            delegate.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (buffered == null) {
            if (delegate == null) {
                delegate = response.getOutputStream();
            }
            delegate.close();
        } else {

            if (delegate == null) {
                delegate = response.getOutputStream();
            }
            delegate.setWriteListener(new WriteListener() {
                @Override
                public void onWritePossible() throws IOException {

                }

                @Override
                public void onError(Throwable t) {
                    log.debug("IOException writing response", t);
                }
            });
            delegate.isReady(); //we need to call this, however it will always return true with the current implementation
            delegate.write(buffered.toByteArray());
            delegate.close();
        }
    }
}
