package org.jboss.resteasy.reactive.server.vertx.test.providers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

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
