package io.quarkus.gcp.functions.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.jboss.logging.Logger;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;
import io.quarkus.runtime.Application;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

public class QuarkusHttpFunction implements HttpFunction {
    private static final Logger LOG = Logger.getLogger(QuarkusHttpFunction.class);
    protected static final String deploymentStatus;
    protected static boolean started = false;

    private static final int BUFFER_SIZE = 8096;

    static {
        StringWriter error = new StringWriter();
        PrintWriter errorWriter = new PrintWriter(error, true);
        if (Application.currentApplication() == null) { // were we already bootstrapped?  Needed for mock unit testing.
            ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
            try {
                // For GCP functions, we need to set the TCCL to the QuarkusHttpFunction classloader then restore it.
                // Without this, we have a lot of classloading issues (ClassNotFoundException on existing classes)
                // during static init.
                Thread.currentThread().setContextClassLoader(QuarkusHttpFunction.class.getClassLoader());
                Class<?> appClass = Class.forName("io.quarkus.runner.ApplicationImpl");
                String[] args = {};
                Application app = (Application) appClass.getConstructor().newInstance();
                app.start(args);
                errorWriter.println("Quarkus bootstrapped successfully.");
                started = true;
            } catch (Exception ex) {
                errorWriter.println("Quarkus bootstrap failed.");
                ex.printStackTrace(errorWriter);
            } finally {
                Thread.currentThread().setContextClassLoader(currentCl);
            }
        } else {
            errorWriter.println("Quarkus bootstrapped successfully.");
            started = true;
        }
        deploymentStatus = error.toString();
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        if (!started) {
            throw new IOException(deploymentStatus);
        }
        dispatch(request, response);
    }

    private void dispatch(HttpRequest request, HttpResponse response) throws IOException {
        try {
            nettyDispatch(request, response);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void nettyDispatch(HttpRequest request, HttpResponse response)
            throws InterruptedException, IOException, ExecutionException {
        String path = request.getPath();
        Optional<String> host = request.getFirstHeader("Host");
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.getMethod()), request.getQuery().map(q -> path + "?" + q).orElse(path));
        if (host.isPresent()) {
            nettyRequest.headers().set("Host", host.get());
        }
        for (Map.Entry<String, List<String>> header : request.getHeaders().entrySet()) {
            nettyRequest.headers().add(header.getKey(), header.getValue());
        }

        HttpContent requestContent = LastHttpContent.EMPTY_LAST_CONTENT;
        if (request.getContentLength() != 0) {
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                int nRead;
                byte[] data = new byte[BUFFER_SIZE];
                while ((nRead = request.getInputStream().read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                byte[] byteArray = buffer.toByteArray();
                ByteBuf body = Unpooled.wrappedBuffer(byteArray);
                requestContent = new DefaultLastHttpContent(body);
            }
        }

        ResponseHandler handler = new ResponseHandler(response);
        VirtualClientConnection<?> connection = VirtualClientConnection.connect(handler, VertxHttpRecorder.VIRTUAL_HTTP);
        connection.sendMessage(nettyRequest);
        connection.sendMessage(requestContent);
        try {
            handler.future.get();
        } finally {
            connection.close();
        }
    }

    private static class ResponseHandler implements VirtualResponseHandler {

        ByteArrayOutputStream baos;
        WritableByteChannel byteChannel;
        CompletableFuture<Void> future = new CompletableFuture<>();
        final HttpResponse response;

        public ResponseHandler(HttpResponse response) {
            this.response = response;
        }

        @Override
        public void handleMessage(Object msg) {
            try {
                if (msg instanceof io.netty.handler.codec.http.HttpResponse) {
                    io.netty.handler.codec.http.HttpResponse res = (io.netty.handler.codec.http.HttpResponse) msg;
                    response.setStatusCode(res.status().code(), res.status().reasonPhrase());
                    for (Map.Entry<String, String> entry : res.headers()) {
                        response.appendHeader(entry.getKey(), entry.getValue());
                    }
                }
                if (msg instanceof HttpContent) {
                    HttpContent content = (HttpContent) msg;
                    if (baos == null) {
                        baos = new ByteArrayOutputStream(BUFFER_SIZE);
                    }
                    int readable = content.content().readableBytes();
                    for (int i = 0; i < readable; i++) {
                        baos.write(content.content().readByte());
                    }
                }
                if (msg instanceof FileRegion) {
                    FileRegion file = (FileRegion) msg;
                    if (file.count() > 0 && file.transferred() < file.count()) {
                        if (baos == null) {
                            baos = new ByteArrayOutputStream(BUFFER_SIZE);
                        }
                        if (byteChannel == null) {
                            byteChannel = Channels.newChannel(baos);
                        }
                        file.transferTo(byteChannel, file.transferred());
                    }
                }
                if (msg instanceof LastHttpContent) {
                    baos.writeTo(response.getOutputStream());
                    try {
                        baos.close();
                    } catch (IOException e) {
                        LOG.warn("Unable to close the ByteArrayOutputStream", e);
                    }
                    future.complete(null);
                }
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void close() {
            if (!future.isDone()) {
                future.completeExceptionally(new RuntimeException("Connection closed"));
            }
        }
    }
}
