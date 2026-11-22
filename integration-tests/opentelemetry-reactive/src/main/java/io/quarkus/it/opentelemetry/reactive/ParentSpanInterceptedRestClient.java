package io.quarkus.it.opentelemetry.reactive;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * A writer interceptor makes the REST Client serialize an {@link InputStream} body on a worker thread instead of
 * streaming it.
 */
@RegisterRestClient(configKey = "client")
@RegisterProvider(ParentSpanInterceptedRestClient.PassThroughInterceptor.class)
@Path("/parent-span")
public interface ParentSpanInterceptedRestClient {

    @POST
    @Path("/stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    byte[] stream(InputStream body);

    class PassThroughInterceptor implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.proceed();
        }
    }
}
