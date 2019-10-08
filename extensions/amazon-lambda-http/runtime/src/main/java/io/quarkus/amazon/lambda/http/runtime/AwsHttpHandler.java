package io.quarkus.amazon.lambda.http.runtime;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

public class AwsHttpHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest request, Context context) {
        VirtualClientConnection connection = VirtualClientConnection.connect(VertxHttpRecorder.VIRTUAL_HTTP);
        try {
            return nettyDispatch(connection, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            connection.close();
        }
    }

    private AwsProxyResponse nettyDispatch(VirtualClientConnection connection,
            AwsProxyRequest request)
            throws Exception {
        String path = request.getPath();
        if (request.getMultiValueQueryStringParameters() != null && !request.getMultiValueQueryStringParameters().isEmpty()) {
            StringBuilder sb = new StringBuilder(path);
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, List<String>> e : request.getMultiValueQueryStringParameters().entrySet()) {
                for (String v : e.getValue()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append("&");
                    }
                    if (request.getRequestSource() == AwsProxyRequest.RequestSource.ALB) {
                        sb.append(e.getKey());
                        sb.append("=");
                        sb.append(v);
                    } else {
                        sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.name()));
                        sb.append("=");
                        sb.append(URLEncoder.encode(v, StandardCharsets.UTF_8.name()));
                    }
                }
            }
            path = sb.toString();
        }
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.getHttpMethod()), path);
        for (Map.Entry<String, List<String>> header : request.getMultiValueHeaders().entrySet()) {
            nettyRequest.headers().add(header.getKey(), header.getValue());
        }
        if (!nettyRequest.headers().contains(HttpHeaderNames.HOST)) {
            nettyRequest.headers().add(HttpHeaderNames.HOST, "localhost");
        }

        HttpContent requestContent = LastHttpContent.EMPTY_LAST_CONTENT;
        if (request.getBody() != null) {
            if (request.isBase64Encoded()) {
                ByteBuf body = Unpooled.wrappedBuffer(Base64.getMimeDecoder().decode(request.getBody()));
                requestContent = new DefaultLastHttpContent(body);
            } else {
                ByteBuf body = Unpooled.copiedBuffer(request.getBody(), StandardCharsets.UTF_8); //TODO: do we need to look at the request encoding?
                requestContent = new DefaultLastHttpContent(body);
            }
        }

        connection.sendMessage(nettyRequest);
        connection.sendMessage(requestContent);
        AwsProxyResponse responseBuilder = new AwsProxyResponse();
        ByteArrayOutputStream baos = null;
        for (;;) {
            // todo should we timeout? have a timeout config?
            //log.info("waiting for message");
            Object msg = connection.queue().poll(100, TimeUnit.MILLISECONDS);
            try {
                if (msg == null)
                    continue;
                //log.info("Got message: " + msg.getClass().getName());

                if (msg instanceof HttpResponse) {
                    HttpResponse res = (HttpResponse) msg;
                    responseBuilder.setStatusCode(res.status().code());
                    responseBuilder.setStatusDescription(res.status().reasonPhrase());
                    responseBuilder.setMultiValueHeaders(new Headers());
                    for (String name : res.headers().names()) {
                        for (String v : res.headers().getAll(name)) {
                            responseBuilder.getMultiValueHeaders().add(name, v);
                        }
                    }
                }
                if (msg instanceof HttpContent) {
                    HttpContent content = (HttpContent) msg;
                    if (baos == null) {
                        // todo what is right size?
                        baos = new ByteArrayOutputStream(500);
                    }
                    int readable = content.content().readableBytes();
                    for (int i = 0; i < readable; i++) {
                        baos.write(content.content().readByte());
                    }
                }
                if (msg instanceof LastHttpContent) {
                    String contentType = responseBuilder.getMultiValueHeaders().getFirst("Content-Type");
                    //TODO: big hack, we should handle charset properly, base64 is always safe though
                    boolean requiresEncoding = true;
                    if (contentType != null) {
                        String ct = contentType.toLowerCase();
                        requiresEncoding = !ct.contains("charset=utf-8") && !ct.contains("json");
                    }
                    if (requiresEncoding) {
                        responseBuilder.setBase64Encoded(true);
                        responseBuilder.setBody(Base64.getMimeEncoder().encodeToString(baos.toByteArray()));
                    } else {
                        responseBuilder.setBase64Encoded(false);
                        responseBuilder.setBody(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                    }
                    return responseBuilder;
                }
            } finally {
                if (msg != null)
                    ReferenceCountUtil.release(msg);
            }
        }
    }
}
