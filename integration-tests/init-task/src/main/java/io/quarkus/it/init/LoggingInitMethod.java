package io.quarkus.it.init;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.PreStart;

public class LoggingInitMethod implements Runnable {

    static Logger LOG = Logger.getLogger(LoggingInitMethod.class);

    @PreStart
    public void run() {
        LOG.info("Message from annotated initialization method");
    }
}
