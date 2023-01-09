package io.quarkus.resteasy.reactive.server.test.providers;

import java.io.IOException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptorContext;

@WithWriterInterceptor
@Provider
public class WriterInterceptor implements jakarta.ws.rs.ext.WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        System.err.println("Around write start");
        context.proceed();
        System.err.println("Around write end");
    }
}
