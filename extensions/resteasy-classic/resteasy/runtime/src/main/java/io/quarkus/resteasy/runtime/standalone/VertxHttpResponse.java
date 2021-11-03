package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpResponse implements HttpResponse {
    private int status = 200;
    private OutputStream os;
    private MultivaluedMap<String, Object> outputHeaders;
    final HttpServerRequest request;
    final HttpServerResponse response;
    private boolean committed;
    private boolean finished;
    private ResteasyProviderFactory providerFactory;
    private final HttpMethod method;
    private final VertxOutput output;
    private final RoutingContext routingContext;

    public VertxHttpResponse(HttpServerRequest request, ResteasyProviderFactory providerFactory,
            final HttpMethod method, BufferAllocator allocator, VertxOutput output, RoutingContext routingContext) {
        this.routingContext = routingContext;
        outputHeaders = new MultivaluedHashMap<String, Object>();
        this.method = method;
        os = (method == null || !method.equals(HttpMethod.HEAD)) ? new VertxOutputStream(this, allocator)
                : null;
        this.request = request;
        this.response = request.response();
        this.providerFactory = providerFactory;
        this.output = output;
    }

    @Override
    public void setOutputStream(OutputStream os) {
        this.os = os;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public MultivaluedMap<String, Object> getOutputHeaders() {
        return outputHeaders;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return os;
    }

    @Override
    public void addNewCookie(NewCookie cookie) {
        outputHeaders.add(javax.ws.rs.core.HttpHeaders.SET_COOKIE, cookie);
    }

    void checkException() throws IOException {
        // todo from old code, do we still need it?
    }

    @Override
    public void sendError(int status) throws IOException {
        checkException();
        sendError(status, null);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        checkException();
        if (committed) {
            throw new IllegalStateException();
        }
        response.setStatusCode(status);
        if (message != null) {
            response.end(message);
        } else {
            response.end();
        }
        committed = true;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void reset() {
        if (committed) {
            throw new IllegalStateException("Response already committed");
        }
        outputHeaders.clear();
    }

    private void transformHeaders() {
        getOutputHeaders().forEach(this::transformHeadersList);
    }

    private void transformHeadersList(final String key, final List<Object> valueList) {
        final MultiMap headers = response.headers();
        for (Object value : valueList) {
            if (value == null) {
                headers.add(key, "");
            } else {
                RuntimeDelegate.HeaderDelegate delegate = providerFactory.getHeaderDelegate(value.getClass());
                if (delegate != null) {
                    headers.add(key, delegate.toString(value));
                } else {
                    headers.add(key, value.toString());
                }
            }
        }
    }

    public void finish() throws IOException {
        checkException();
        if (finished || response.ended())
            return;
        try {
            if (os != null) {
                os.close(); // this will end() vertx response
            } else {
                committed = true;
                response.setStatusCode(getStatus());
                transformHeaders();
                routingContext.addHeadersEndHandler(h -> {
                    response.headers().remove(HttpHeaders.CONTENT_LENGTH);
                    response.headers().set(HttpHeaders.CONNECTION, HttpHeaders.KEEP_ALIVE);
                });
                response.end();
            }
        } finally {
            finished = true;
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        checkException();
        if (os != null) {
            os.flush();
        }
    }

    public void writeBlocking(ByteBuf buffer, boolean finished) throws IOException {
        checkException();
        prepareWrite(buffer, finished);
        output.write(buffer, finished);
    }

    public CompletionStage<Void> writeNonBlocking(ByteBuf buffer, boolean finished) {
        try {
            prepareWrite(buffer, finished);
        } catch (IOException e) {
            CompletableFuture<Void> ret = new CompletableFuture<>();
            ret.completeExceptionally(e);
            return ret;
        }
        return output.writeNonBlocking(buffer, finished);
    }

    private void prepareWrite(ByteBuf buffer, boolean finished) throws IOException {
        if (!isCommitted()) {
            committed = true;
            response.setStatusCode(getStatus());
            transformHeaders();
            if (finished) {
                boolean explicitChunked = "chunked".equalsIgnoreCase(response.headers().get("transfer-encoding"));
                if (!explicitChunked) {
                    if (buffer == null) {
                        getOutputHeaders().putSingle(javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH, "0");
                    } else {
                        getOutputHeaders().putSingle(javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH, "" + buffer.readableBytes());
                    }
                }
            } else if (!response.headers().contains(javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH)) {
                response.setChunked(true);
            }
        }
        if (finished)
            this.finished = true;
    }

}
