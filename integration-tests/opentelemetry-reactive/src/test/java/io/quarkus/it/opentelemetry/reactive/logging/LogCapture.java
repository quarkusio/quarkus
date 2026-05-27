package io.quarkus.it.opentelemetry.reactive.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogCapture {

    public static final List<LogRecord> records = new CopyOnWriteArrayList<>();

    private static volatile boolean installed = false;

    public static synchronized void install() {
        if (installed) {
            return;
        }
        Handler handler = new Handler() {
            {
                setLevel(Level.ALL);
            }

            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger.getLogger("io.quarkus.rest.logging").addHandler(handler);
        installed = true;
    }
}
