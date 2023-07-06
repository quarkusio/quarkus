package io.quarkus.it.init;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.PreStart;

@PreStart("logging-runnable")
public class LoggingInitRunnable implements Runnable {

    static Logger LOG = Logger.getLogger(LoggingInitRunnable.class);

    public void run() {
        LOG.info("Message from annotated initialization Runnable");
    }
}
