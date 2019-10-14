package io.quarkus.amazon.sam;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.runtime.Application;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

@SuppressWarnings("unused")
public class SamHandler implements RequestStreamHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SamHandler.class);

    private static ObjectMapper objectMapper = new ObjectMapper();
    private ObjectReader objectReader = objectMapper.readerFor(AwsProxyRequest.class);
    private ObjectWriter objectWriter = objectMapper.writerFor(AwsProxyResponse.class);

    static {
        objectMapper.registerModule(new AfterburnerModule());
    }

    static {
        StringWriter error = new StringWriter();
        PrintWriter errorWriter = new PrintWriter(error, true);
        if (Application.currentApplication() == null) {
            try {
                Class appClass = Class.forName("io.quarkus.runner.ApplicationImpl1");
                String[] args = {};
                Application app = (Application) appClass.newInstance();
                app.start(args);
                errorWriter.println("Quarkus bootstrapped successfully.");
            } catch (Exception ex) {
                errorWriter.println("Quarkus bootstrap failed.");
                ex.printStackTrace(errorWriter);
            }
        } else {
            errorWriter.println("Quarkus bootstrapped successfully.");
        }
    }

    private AwsProxyExceptionHandler exceptionHandler = new AwsProxyExceptionHandler();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        try {
            AwsProxyRequest request = objectReader.readValue(inputStream);
            AwsProxyResponse resp = proxy(request);

            objectWriter.writeValue(outputStream, resp);
        } catch (JsonParseException e) {
            LOG.error("Error while parsing request object stream", e);
            objectMapper.writeValue(outputStream, exceptionHandler.handle(e));
        } catch (JsonMappingException e) {
            LOG.error("Error while mapping object to RequestType class", e);
            objectMapper.writeValue(outputStream, exceptionHandler.handle(e));
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    private AwsProxyResponse proxy(AwsProxyRequest request) {
        try {
            VirtualClientConnection connection = VirtualClientConnection.connect(VertxHttpRecorder.VIRTUAL_HTTP);

            connection.sendMessage(buildRequest(request));
            connection.sendMessage(buildContent(request));

            return buildResponse(connection);
        } catch (Exception e) {
            LOG.error("Error while handling request", e);

            return exceptionHandler.handle(e);
        }
    }

    private AwsProxyResponse buildResponse(VirtualClientConnection connection) throws InterruptedException {
        AwsProxyResponse response = null;
        ByteArrayOutputStream baos = null;
        for (;;) {
            // todo should we timeout? have a timeout config?
            //log.info("waiting for message");
            Object msg = connection.queue().poll(100, TimeUnit.MILLISECONDS);
            try {
                if (msg == null)
                    continue;

                if (msg instanceof HttpResponse) {
                    HttpResponse res = (HttpResponse) msg;
                    response = new AwsProxyResponse(res.status().code());
                    for (Map.Entry<String, String> entry : res.headers()) {
                        response.addHeader(entry.getKey(), entry.getValue());
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
                    response.setBody(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                    return response;
                }
            } finally {
                if (msg != null)
                    ReferenceCountUtil.release(msg);
            }
        }
    }

    private int getPort(AwsProxyRequest request) {
        if (request.getMultiValueHeaders() == null) {
            return 443;
        }
        String port = request.getMultiValueHeaders().getFirst("X-Forwarded-Port");
        return SecurityUtils.isValidPort(port) ? Integer.parseInt(port) : 443;
    }

    private DefaultHttpRequest buildRequest(AwsProxyRequest request) {

        String path = request.getPath();
        String query = request.getQueryString();
        if (query != null && !"".equals(query.trim())) {
            path = path + '?' + query;
        }
        String host = request.getMultiValueHeaders().getFirst(HttpHeaders.HOST);
        if (host.matches(".*:[0-9]+")) {
            host = host.substring(0, host.indexOf(':'));
        }
        int port = getPort(request);
        if (port != -1) {
            host = host + ':' + port;
        }
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.getHttpMethod()), path);
        nettyRequest.headers().set("Host", host);
        for (Map.Entry<String, List<String>> header : request.getMultiValueHeaders().entrySet()) {
            nettyRequest.headers().add(header.getKey(), header.getValue());
        }

        return nettyRequest;
    }

    private HttpContent buildContent(AwsProxyRequest request) {
        String body = request.getBody();
        return body != null
                ? new DefaultLastHttpContent(wrappedBuffer(body.getBytes(StandardCharsets.UTF_8)))
                : LastHttpContent.EMPTY_LAST_CONTENT;
    }
}
