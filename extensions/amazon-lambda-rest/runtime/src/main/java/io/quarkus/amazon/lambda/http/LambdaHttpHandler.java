package io.quarkus.amazon.lambda.http;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

@SuppressWarnings("unused")
public class LambdaHttpHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private static final Logger log = Logger.getLogger("quarkus.amazon.lambda.http");

    private static final Headers errorHeaders = new Headers();

    static {
        errorHeaders.putSingle("Content-Type", "application/json");
    }

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest request, Context context) {
        try {
            InetSocketAddress clientAddress = getClientAddressFromRequest(request);
            return nettyDispatch(clientAddress, request, context);
        } catch (Exception e) {
            log.error("Request Failure", e);
            return new AwsProxyResponse(500, errorHeaders, "{ \"message\": \"Internal Server Error\" }");
        }
    }

    private InetSocketAddress getClientAddressFromRequest(AwsProxyRequest request) {
        if (request.getRequestContext() != null && request.getRequestContext().getIdentity() != null) {
            if (request.getRequestContext().getIdentity().getSourceIp() != null) {
                return new InetSocketAddress(request.getRequestContext().getIdentity().getSourceIp(), 443);
            }
        }
        return null;
    }

    private AwsProxyResponse nettyDispatch(InetSocketAddress clientAddress, AwsProxyRequest request, Context context)
            throws Exception {
        QuarkusHttpHeaders quarkusHeaders = new QuarkusHttpHeaders();
        quarkusHeaders.setContextObject(Context.class, context);
        quarkusHeaders.setContextObject(AwsProxyRequestContext.class, request.getRequestContext());
        quarkusHeaders.setContextObject(AwsProxyRequest.class, request);

        NettyResponseHandler handler = new NettyResponseHandler(request);
        VirtualClientConnection connection = VirtualClientConnection
                .connect(handler, VertxHttpRecorder.VIRTUAL_HTTP, clientAddress);

        connection.sendMessage(createHttpRequestHead(request, quarkusHeaders));
        connection.sendMessage(createHttpRequestBody(request));
        try {
            return handler.getFuture().get();
        } finally {
            connection.close();
        }
    }

    private HttpRequest createHttpRequestHead(AwsProxyRequest request, QuarkusHttpHeaders quarkusHeaders)
            throws UnsupportedEncodingException {
        String pathWithQueryString = getPathWithQueryStringFromRequest(request);
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.getHttpMethod()), pathWithQueryString, quarkusHeaders);

        HttpHeaders nettyRequestHeaders = nettyRequest.headers();
        if (request.getMultiValueHeaders() != null) { //apparently this can be null if no headers are sent
            for (Map.Entry<String, List<String>> header : request.getMultiValueHeaders().entrySet()) {
                nettyRequestHeaders.add(header.getKey(), header.getValue());
            }
        }
        if (!nettyRequestHeaders.contains(HttpHeaderNames.HOST)) {
            nettyRequestHeaders.add(HttpHeaderNames.HOST, "localhost");
        }
        return nettyRequest;
    }

    private HttpContent createHttpRequestBody(AwsProxyRequest request) {
        if (request.getBody() != null) {
            ByteBuf body;
            if (request.isBase64Encoded()) {
                body = Unpooled.wrappedBuffer(Base64.getMimeDecoder().decode(request.getBody()));
            } else {
                //TODO: do we need to look at the request encoding?
                body = Unpooled.copiedBuffer(request.getBody(), StandardCharsets.UTF_8);
            }
            return new DefaultLastHttpContent(body);
        } else {
            return LastHttpContent.EMPTY_LAST_CONTENT;
        }
    }

    private String getPathWithQueryStringFromRequest(AwsProxyRequest request) throws UnsupportedEncodingException {
        if (hasQueryParameters(request)) {
            return getPathWithQueryParameters(request);
        } else {
            return request.getPath();
        }
    }

    private boolean hasQueryParameters(AwsProxyRequest request) {
        return request.getMultiValueQueryStringParameters() != null && !request.getMultiValueQueryStringParameters().isEmpty();
    }

    private String getPathWithQueryParameters(AwsProxyRequest request) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(request.getPath());
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
                    sb.append(urlEncode(e.getKey()));
                    sb.append("=");
                    sb.append(urlEncode(v));
                }
            }
        }
        return sb.toString();
    }

    private String urlEncode(String key) throws UnsupportedEncodingException {
        return URLEncoder.encode(key, StandardCharsets.UTF_8.name());
    }
}
