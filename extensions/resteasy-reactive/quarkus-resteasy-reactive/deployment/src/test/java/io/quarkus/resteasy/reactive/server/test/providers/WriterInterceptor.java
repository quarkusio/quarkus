package io.quarkus.resteasy.reactive.server.test.providers;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptorContext;

@WithWriterInterceptor
@Provider
public class WriterInterceptor implements javax.ws.rs.ext.WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        System.err.println("Around write start");
        context.proceed();
        System.err.println("Around write end");
    }
}
