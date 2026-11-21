package io.quarkus.it.virtual;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

import io.quarkus.azure.functions.resteasy.runtime.Function;

final class FunctionInvoker {

    private static final ExecutionContext CONTEXT = new ExecutionContext() {
        @Override
        public Logger getLogger() {
            return null;
        }

        @Override
        public String getInvocationId() {
            return null;
        }

        @Override
        public String getFunctionName() {
            return null;
        }
    };

    private FunctionInvoker() {
    }

    static HttpResponseMessage get(String path, String acceptEncoding) {
        HttpRequestMessageMock request = new HttpRequestMessageMock();
        request.setUri(URI.create("https://foo.com" + path));
        request.setHttpMethod(HttpMethod.GET);
        if (acceptEncoding != null) {
            request.getHeaders().put("Accept-Encoding", acceptEncoding);
        }
        HttpResponseMessage response = new Function().run(request, CONTEXT);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeader("Transfer-Encoding")).isNull();
        String contentLength = response.getHeader("Content-Length");
        if (contentLength != null) {
            assertThat(Integer.parseInt(contentLength)).isEqualTo(((byte[]) response.getBody()).length);
        }
        return response;
    }

    static String body(HttpResponseMessage response) throws IOException {
        try (InputStream in = decode(response.getHeader("Content-Encoding"), (byte[]) response.getBody())) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String staticFileBody() throws IOException {
        try (InputStream in = FunctionInvoker.class.getResourceAsStream("/META-INF/resources/compression.txt")) {
            assertThat(in).as("compression.txt fixture").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream decode(String contentEncoding, byte[] body) throws IOException {
        if ("gzip".equals(contentEncoding)) {
            return new GZIPInputStream(new ByteArrayInputStream(body));
        }
        if ("deflate".equals(contentEncoding)) {
            return new InflaterInputStream(new ByteArrayInputStream(body));
        }
        assertThat(contentEncoding).isNull();
        return new ByteArrayInputStream(body);
    }
}
