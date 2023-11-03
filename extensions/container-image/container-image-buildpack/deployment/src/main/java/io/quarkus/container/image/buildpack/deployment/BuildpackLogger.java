package io.quarkus.container.image.buildpack.deployment;

import org.jboss.logging.Logger;

public class BuildpackLogger implements dev.snowdrop.buildpack.Logger {

    private static final Logger bplog = Logger.getLogger("buildpack");

    private String trim(String message) {
        if (message.endsWith("\n")) {
            message = message.substring(0, message.length() - 1);
        }
        if (message.endsWith("\r")) {
            message = message.substring(0, message.length() - 1);
        }
        return message;
    }

    @Override
    public void stdout(String message) {
        bplog.info(trim(prepare(message)));
    }

    @Override
    public void stderr(String message) {
        bplog.error(trim(prepare(message)));
    }
}
