package org.jboss.resteasy.reactive.server.spi;

public interface StreamingResponse<T extends StreamingResponse<T>> {

    T setStatusCode(int code);

    T setResponseHeader(CharSequence name, CharSequence value);

    T setResponseHeader(CharSequence name, Iterable<CharSequence> values);
}
