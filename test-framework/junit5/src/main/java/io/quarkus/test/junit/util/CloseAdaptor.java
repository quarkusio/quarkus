package io.quarkus.test.junit.util;

import org.junit.jupiter.api.extension.ExtensionContext;

public class CloseAdaptor implements ExtensionContext.Store.CloseableResource {

    final AutoCloseable closeable;

    public CloseAdaptor(AutoCloseable closeable) {
        this.closeable = closeable;
    }

    @Override
    public void close() throws Throwable {
        closeable.close();
    }
}
