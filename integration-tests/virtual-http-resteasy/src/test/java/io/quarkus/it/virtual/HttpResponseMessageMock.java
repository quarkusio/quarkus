package io.quarkus.it.virtual;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;

/**
 * The mock for HttpResponseMessage, can be used in unit tests to verify if the
 * returned response by HTTP trigger function is correct or not.
 */
public class HttpResponseMessageMock implements HttpResponseMessage {
    private int httpStatusCode;
    private HttpStatusType httpStatus;
    private Object body;
    private Map<String, String> headers;

    public HttpResponseMessageMock(final HttpStatusType status, final Map<String, String> headers, final Object body) {
        this.httpStatus = status;
        this.httpStatusCode = status.value();
        this.headers = headers;
        this.body = body;
    }

    @Override
    public HttpStatusType getStatus() {
        return this.httpStatus;
    }

    @Override
    public int getStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String getHeader(String key) {
        return this.headers.get(key);
    }

    @Override
    public Object getBody() {
        return this.body;
    }

    public static class HttpResponseMessageBuilderMock implements HttpResponseMessage.Builder {
        private Object body;
        private int httpStatusCode;
        private Map<String, String> headers = new HashMap<>();
        private HttpStatusType httpStatus;

        public Builder status(HttpStatus status) {
            this.httpStatusCode = status.value();
            this.httpStatus = status;
            return this;
        }

        @Override
        public Builder status(final HttpStatusType httpStatusType) {
            this.httpStatusCode = httpStatusType.value();
            this.httpStatus = httpStatusType;
            return this;
        }

        @Override
        public HttpResponseMessage.Builder header(final String key, final String value) {
            this.headers.put(key, value);
            return this;
        }

        @Override
        public HttpResponseMessage.Builder body(final Object body) {
            this.body = body;
            return this;
        }

        @Override
        public HttpResponseMessage build() {
            return new HttpResponseMessageMock(this.httpStatus, this.headers, this.body);
        }
    }
}
