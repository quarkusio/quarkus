package org.acme.funqy.cloudevent;

import io.quarkus.funqy.Funq;
import org.jboss.logging.Logger;

public class CloudEventGreeting {
    private static final Logger log = Logger.getLogger(CloudEventGreeting.class);

    @Funq
    public void myCloudEventGreeting(Person input) {
        log.info("Hello " + input.getName());
    }
}
