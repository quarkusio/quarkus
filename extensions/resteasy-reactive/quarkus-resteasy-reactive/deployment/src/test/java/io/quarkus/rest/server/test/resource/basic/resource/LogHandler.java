package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {

    public static final String MESSAGE_CODE = "RESTEASY002142";

    private static volatile int messagesLogged = 0;

    @Override
    public void publish(LogRecord record) {
        if (record.getMessage().contains(MESSAGE_CODE)) {
            messagesLogged++;
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    public int getMessagesLogged() {
        return messagesLogged;
    }
}
