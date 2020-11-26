package org.jboss.resteasy.reactive.server.handlers;

import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class InterceptorHandler implements ServerRestHandler {

    private final WriterInterceptor[] writerInterceptors;
    private final ReaderInterceptor[] readerInterceptors;

    public InterceptorHandler(WriterInterceptor[] writerInterceptors, ReaderInterceptor[] readerInterceptors) {
        this.writerInterceptors = writerInterceptors;
        this.readerInterceptors = readerInterceptors;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        requestContext.setWriterInterceptors(writerInterceptors);
        requestContext.setReaderInterceptors(readerInterceptors);
    }
}
