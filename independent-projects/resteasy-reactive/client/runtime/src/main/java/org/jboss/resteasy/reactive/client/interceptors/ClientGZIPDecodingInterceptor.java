package org.jboss.resteasy.reactive.client.interceptors;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_ENCODING;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

/**
 * Implementation based on {@see org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor}.
 */
public class ClientGZIPDecodingInterceptor implements ReaderInterceptor {

    private static final String GZIP = "gzip";

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context)
            throws IOException, WebApplicationException {
        Object encoding = context.getHeaders().getFirst(CONTENT_ENCODING);
        if (encoding != null && encoding.toString().equalsIgnoreCase(GZIP)) {
            InputStream old = context.getInputStream();
            GZIPInputStream is = new GZIPInputStream(old);
            context.setInputStream(is);

            Object response;
            try {
                response = context.proceed();
            } finally {
                context.setInputStream(old);
            }

            return response;
        } else {
            return context.proceed();
        }
    }
}
