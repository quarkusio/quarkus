package io.quarkus.vertx.http.deployment.devmode.console;

import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.quarkus.dev.devui.DevConsoleRequest;
import io.quarkus.dev.devui.DevConsoleResponse;
import io.quarkus.netty.runtime.virtual.VirtualAddress;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;

/**
 * The idea here is to dispatch the DevConsoleRequest into the Netty event loop that powers the Dev Vert.x instance.
 *
 * Although this is loaded by the Augmentation ClassLoader at build time,
 * it however works because all of the classes part its signature are loaded by the System ClassLoader
 * which is the same for build steps and runtime code (DevConsoleRequest is a part of the devmode-spi which
 * is a parent first dependency thus also loaded by the System ClassLoader).
 */
@SuppressWarnings("unused")
public class DevConsoleHttpHandler implements Consumer<DevConsoleRequest> {
    private static final Logger log = Logger.getLogger(DevConsoleHttpHandler.class);
    public static VirtualAddress QUARKUS_DEV_CONSOLE = new VirtualAddress("quarkus-dev-console");

    private static final int BUFFER_SIZE = 8096;

    @Override
    public void accept(DevConsoleRequest request) {
        try {
            nettyDispatch(request);
        } catch (Exception e) {
            request.getResponse().completeExceptionally(e);
        }

    }

    private class NettyResponseHandler implements VirtualResponseHandler {
        ByteArrayOutputStream baos;
        WritableByteChannel byteChannel;
        final DevConsoleRequest request;
        final DevConsoleResponse responseBuilder = new DevConsoleResponse();
        private volatile VirtualClientConnection connection;

        public NettyResponseHandler(DevConsoleRequest request) {
            this.request = request;
        }

        public CompletableFuture<DevConsoleResponse> getFuture() {
            return request.getResponse();
        }

        @Override
        public void handleMessage(Object msg) {
            try {
                //log.info("Got message: " + msg.getClass().getName());

                if (msg instanceof HttpResponse) {
                    HttpResponse res = (HttpResponse) msg;
                    responseBuilder.setStatus(res.status().code());

                    for (String name : res.headers().names()) {
                        responseBuilder.getHeaders().put(name, res.headers().getAll(name));
                    }
                }
                if (msg instanceof HttpContent) {
                    HttpContent content = (HttpContent) msg;
                    int readable = content.content().readableBytes();
                    if (baos == null && readable > 0) {
                        baos = createByteStream();
                    }
                    for (int i = 0; i < readable; i++) {
                        baos.write(content.content().readByte());
                    }
                }
                if (msg instanceof FileRegion) {
                    FileRegion file = (FileRegion) msg;
                    if (file.count() > 0 && file.transferred() < file.count()) {
                        if (baos == null)
                            baos = createByteStream();
                        if (byteChannel == null)
                            byteChannel = Channels.newChannel(baos);
                        file.transferTo(byteChannel, file.transferred());
                    }
                }
                if (msg instanceof LastHttpContent) {
                    if (baos != null) {
                        responseBuilder.setBody(baos.toByteArray());
                    }
                    getFuture().complete(responseBuilder);
                    if (connection != null) {
                        connection.close();
                    }
                }
            } catch (Throwable ex) {
                getFuture().completeExceptionally(ex);
            } finally {
                if (msg != null) {
                    ReferenceCountUtil.release(msg);
                }
            }
        }

        @Override
        public void close() {
            if (!getFuture().isDone()) {
                getFuture().completeExceptionally(new RuntimeException("Connection closed"));
            }
        }

        public void setConnection(VirtualClientConnection connection) {
            this.connection = connection;
        }

        public VirtualClientConnection getConnection() {
            return connection;
        }
    }

    private void nettyDispatch(DevConsoleRequest request)
            throws Exception {
        String uri = request.getUri();
        QuarkusHttpHeaders quarkusHeaders = new QuarkusHttpHeaders();
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.getMethod()), uri, quarkusHeaders);
        for (Map.Entry<String, List<String>> i : request.getHeaders().entrySet()) {
            nettyRequest.headers().add(i.getKey(), i.getValue());
        }
        if (!nettyRequest.headers().contains(HttpHeaderNames.HOST)) {
            nettyRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        }

        HttpContent requestContent = LastHttpContent.EMPTY_LAST_CONTENT;
        if (request.getBody() != null) {
            ByteBuf body = Unpooled.wrappedBuffer(request.getBody());
            requestContent = new DefaultLastHttpContent(body);
        }
        NettyResponseHandler handler = new NettyResponseHandler(request);
        VirtualClientConnection connection = VirtualClientConnection.connect(handler, QUARKUS_DEV_CONSOLE,
                null);
        handler.setConnection(connection);

        connection.sendMessage(nettyRequest);
        connection.sendMessage(requestContent);
    }

    private ByteArrayOutputStream createByteStream() {
        ByteArrayOutputStream baos;
        baos = new ByteArrayOutputStream(BUFFER_SIZE);
        return baos;
    }

}
