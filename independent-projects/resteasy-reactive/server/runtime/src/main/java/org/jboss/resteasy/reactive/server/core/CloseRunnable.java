package org.jboss.resteasy.reactive.server.core;

import java.io.Closeable;
import java.io.IOException;
import org.jboss.logging.Logger;

public class CloseRunnable implements Runnable {

    private static final Logger log = Logger.getLogger(CloseRunnable.class);

    final Closeable closeable;

    public CloseRunnable(Closeable closeable) {
        this.closeable = closeable;
    }

    @Override
    public void run() {
        try {
            closeable.close();
        } catch (IOException e) {
            log.error("Failed to run close task", e);
        }
    }
}
