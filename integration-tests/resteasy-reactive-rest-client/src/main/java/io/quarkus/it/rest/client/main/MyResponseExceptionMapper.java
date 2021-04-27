package io.quarkus.it.rest.client.main;

import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class MyResponseExceptionMapper implements ResponseExceptionMapper<Exception> {
    @Override
    public Exception toThrowable(Response response) {
        if (response.getStatus() == 422) {
            return new MyException("expected");
        } else {
            return new IllegalStateException("");
        }
    }

    public static class MyException extends Exception {
        public MyException(String message) {
            super(message);
        }
    }
}
