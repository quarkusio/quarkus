package io.quarkus.it.picocli;

import org.jboss.logging.Logger;

import picocli.CommandLine;

@CommandLine.Command(name = "json-logging")
public class JsonLoggingCommand implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(JsonLoggingCommand.class);

    @CommandLine.Parameters(paramLabel = "<name>", defaultValue = "picocli")
    String name;

    @Override
    public void run() {
        LOGGER.infof("Hello %s", name);
    }
}
