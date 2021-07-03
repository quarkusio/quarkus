package io.quarkus.amazon.lambda.http;

import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;

class NettyResponseHandler implements VirtualResponseHandler {
    private static final int BUFFER_SIZE = 8096;

    private final APIGatewayV2HTTPResponse responseBuilder = new APIGatewayV2HTTPResponse();
    private final CompletableFuture<APIGatewayV2HTTPResponse> future = new CompletableFuture<>();
    private final APIGatewayV2HTTPEvent request;
    private ByteArrayOutputStream baos;
    private WritableByteChannel byteChannel;

    public NettyResponseHandler(APIGatewayV2HTTPEvent request) {
        this.request = request;
    }

    public CompletableFuture<APIGatewayV2HTTPResponse> getFuture() {
        return future;
    }

    @Override
    public void handleMessage(Object msg) {
        try {
            //log.info("Got message: " + msg.getClass().getName());

            if (msg instanceof HttpResponse) {
                HttpResponse res = (HttpResponse) msg;
                responseBuilder.setStatusCode(res.status().code());

                final Map<String, String> headers = new HashMap<>();
                responseBuilder.setHeaders(headers);
                for (String name : res.headers().names()) {
                    final List<String> allForName = res.headers().getAll(name);
                    if (allForName == null || allForName.isEmpty()) {
                        continue;
                    }
                    final StringBuilder sb = new StringBuilder();
                    for (Iterator<String> valueIterator = allForName.iterator(); valueIterator.hasNext();) {
                        sb.append(valueIterator.next());
                        if (valueIterator.hasNext()) {
                            sb.append(",");
                        }
                    }
                    headers.put(name, sb.toString());
                }
            }
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;
                int readable = content.content().readableBytes();
                if (baos == null && readable > 0) {
                    baos = new ByteArrayOutputStream(BUFFER_SIZE);
                }
                for (int i = 0; i < readable; i++) {
                    baos.write(content.content().readByte());
                }
            }
            if (msg instanceof FileRegion) {
                FileRegion file = (FileRegion) msg;
                if (file.count() > 0 && file.transferred() < file.count()) {
                    if (baos == null)
                        baos = new ByteArrayOutputStream(BUFFER_SIZE);
                    if (byteChannel == null)
                        byteChannel = Channels.newChannel(baos);
                    file.transferTo(byteChannel, file.transferred());
                }
            }
            if (msg instanceof LastHttpContent) {
                if (baos != null) {
                    if (isBinary(responseBuilder.getHeaders().get("Content-Type"))) {
                        responseBuilder.setIsBase64Encoded(true);
                        responseBuilder.setBody(Base64.getMimeEncoder().encodeToString(baos.toByteArray()));
                    } else {
                        responseBuilder.setBody(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                    }
                }
                future.complete(responseBuilder);
            }
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        } finally {
            if (msg != null) {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    static Set<String> binaryTypes = new HashSet<>();

    static {
        binaryTypes.add("application/octet-stream");
        binaryTypes.add("image/jpeg");
        binaryTypes.add("image/png");
        binaryTypes.add("image/gif");
    }

    private boolean isBinary(String contentType) {
        if (contentType != null) {
            int index = contentType.indexOf(';');
            if (index >= 0) {
                return binaryTypes.contains(contentType.substring(0, index));
            } else {
                return binaryTypes.contains(contentType);
            }
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        if (!future.isDone()) {
            future.completeExceptionally(new RuntimeException("Connection closed"));
        }
    }
}
