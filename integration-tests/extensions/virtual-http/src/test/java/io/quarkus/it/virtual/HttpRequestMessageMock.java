package io.quarkus.it.virtual;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;

public class HttpRequestMessageMock implements HttpRequestMessage<Optional<byte[]>> {
    protected URI uri;
    protected HttpMethod httpMethod;
    protected Map<String, String> headers = new HashMap<>();
    protected Map<String, String> queryParameters = new HashMap<>();
    protected byte[] body;

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    @Override
    public Optional<byte[]> getBody() {
        return Optional.ofNullable(body);
    }

    @Override
    public HttpResponseMessage.Builder createResponseBuilder(HttpStatus httpStatus) {
        return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(httpStatus);
    }

    @Override
    public HttpResponseMessage.Builder createResponseBuilder(HttpStatusType httpStatusType) {
        return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(httpStatusType);
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setQueryParameters(Map<String, String> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
