package io.quarkus.resteasy.reactive.server.runtime;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.resteasy.reactive.server.Closer;

public class CloserImpl implements Closer {

    private static final Logger log = Logger.getLogger(CloserImpl.class);

    private final List<Closeable> closables = new ArrayList<>();

    @Override
    public void add(Closeable c) {
        closables.add(c);
    }

    void close() {
        for (Closeable closable : closables) {
            try {
                closable.close();
            } catch (Exception e) {
                log.warn("Unable to perform close operation", e);
            }
        }
    }
}
