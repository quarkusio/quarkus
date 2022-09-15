package io.quarkus.rest.client.reactive.runtime;

import java.io.Closeable;
import java.io.IOException;

import jakarta.annotation.PreDestroy;

import org.jboss.logging.Logger;

import io.quarkus.arc.NoClassInterceptors;
import io.quarkus.runtime.MockedThroughWrapper;

public abstract class RestClientReactiveCDIWrapperBase<T extends Closeable> implements Closeable, MockedThroughWrapper {
    private static final Logger log = Logger.getLogger(RestClientReactiveCDIWrapperBase.class);

    private final T delegate;
    private Object mock;

    public RestClientReactiveCDIWrapperBase(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey) {
        this.delegate = RestClientCDIDelegateBuilder.createDelegate(jaxrsInterface, baseUriFromAnnotation, configKey);
    }

    @Override
    @NoClassInterceptors
    public void close() throws IOException {
        if (mock == null) {
            delegate.close();
        }
    }

    @PreDestroy
    @NoClassInterceptors
    public void destroy() {
        try {
            if (mock == null) {
                close();
            }
        } catch (IOException e) {
            log.warn("Failed to close cdi-created rest client instance", e);
        }
    }

    // used by generated code
    @SuppressWarnings("unused")
    @NoClassInterceptors
    public Object getDelegate() {
        return mock == null ? delegate : mock;
    }

    @Override
    @NoClassInterceptors
    public void setMock(Object mock) {
        this.mock = mock;
    }

    @Override
    @NoClassInterceptors
    public void clearMock() {
        this.mock = null;
    }
}
