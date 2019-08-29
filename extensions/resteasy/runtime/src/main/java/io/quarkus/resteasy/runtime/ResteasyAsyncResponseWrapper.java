package io.quarkus.resteasy.runtime;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class ResteasyAsyncResponseWrapper extends HttpServletResponseWrapper {
    private final HttpServletRequest request;
    private ResteasyAsyncOutputStream stream;

    public ResteasyAsyncResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
        super(response);
        this.request = request;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (stream != null) {
            return stream;
        }
        return stream = new ResteasyAsyncOutputStream((HttpServletResponse) getResponse(), request);
    }
}
