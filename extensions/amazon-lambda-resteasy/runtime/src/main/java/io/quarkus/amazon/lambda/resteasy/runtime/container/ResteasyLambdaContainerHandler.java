package io.quarkus.amazon.lambda.resteasy.runtime.container;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;

import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.ExceptionHandler;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

public class ResteasyLambdaContainerHandler<RequestType, ResponseType> extends
        LambdaContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {

    private ResteasyLambdaFilter resteasyFilter;

    public static ResteasyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(
            Map<String, String> initParameters) {
        return new ResteasyLambdaContainerHandler<>(
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                initParameters);
    }

    @SuppressWarnings("unchecked")
    private ResteasyLambdaContainerHandler(Class<ResponseType> responseTypeClass,
            RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
            ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
            SecurityContextWriter<RequestType> securityContextWriter,
            ExceptionHandler<ResponseType> exceptionHandler,
            Map<String, String> initParameters) {
        super((Class<RequestType>) AwsProxyResteasyRequest.class, responseTypeClass, requestReader, responseWriter,
                securityContextWriter, exceptionHandler);
        initialize();
    }

    @Override
    protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest request, AwsHttpServletResponse response, Context context) {
        VirtualClientConnection connection = VirtualClientConnection.connect(VertxHttpRecorder.VIRTUAL_HTTP);
        try {
            nettyDispatch(connection, request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } finally {
            connection.close();
        }
    }

    private void nettyDispatch(VirtualClientConnection connection, AwsProxyHttpServletRequest request,
            AwsHttpServletResponse response) throws Exception {
        String path = request.getPathInfo();

        String query = request.getQueryString();
        if (query != null) {
            path = path + '?' + query;
        }

        String host = request.getRemoteHost();
        if (request.getRemotePort() != -1) {
            host = host + ':' + request.getRemotePort();
        }

        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.getMethod()), path);
        nettyRequest.headers().set("Host", host);
        for (Map.Entry<String, List<String>> header : request.getAwsProxyRequest().getMultiValueHeaders().entrySet()) {
            nettyRequest.headers().add(header.getKey(), header.getValue());
        }

        HttpContent requestContent = LastHttpContent.EMPTY_LAST_CONTENT;
        if (request.getAwsProxyRequest().getBody() != null) {
            String body = request.getAwsProxyRequest().getBody();
            ByteBuf buffer = Unpooled.wrappedBuffer(body.getBytes(UTF_8));
            requestContent = new DefaultLastHttpContent(buffer);
        }

        connection.sendMessage(nettyRequest);
        connection.sendMessage(requestContent);
        ByteArrayOutputStream baos = null;
        boolean last = false;
        while (!last) {
            Object msg = connection.queue().poll(100, TimeUnit.MILLISECONDS);
            if (msg != null) {
                try {
                    if (msg instanceof HttpResponse) {
                        HttpResponse res = (HttpResponse) msg;

                        response.setStatus(res.status().code());
                        for (Map.Entry<String, String> entry : res.headers()) {
                            response.addHeader(entry.getKey(), entry.getValue());
                        }
                    }
                    if (msg instanceof HttpContent) {
                        HttpContent content = (HttpContent) msg;
                        if (baos == null) {
                            baos = new ByteArrayOutputStream(500);
                        }
                        ByteBuf buffer = content.content();

                        while (buffer.readableBytes() > 0) {
                            byte[] bytes = new byte[buffer.readableBytes()];
                            buffer.readBytes(bytes);
                            baos.write(bytes);
                        }
                    }
                    if (msg instanceof LastHttpContent) {
                        response.getOutputStream().write(baos.toByteArray());
                        response.flushBuffer();
                        last = true;
                    }
                } finally {
                    ReferenceCountUtil.release(msg);
                }
            }
        }
    }

    @Override
    public void initialize() {
    }
}
