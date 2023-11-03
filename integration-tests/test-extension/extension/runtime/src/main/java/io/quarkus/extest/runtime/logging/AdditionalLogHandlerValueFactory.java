package io.quarkus.extest.runtime.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AdditionalLogHandlerValueFactory {

    public RuntimeValue<Optional<Handler>> create() {
        return new RuntimeValue<>(Optional.of(new TestHandler()));
    }

    public static class TestHandler extends Handler {

        public final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public Level getLevel() {
            return Level.FINE;
        }

        @Override
        public void close() throws SecurityException {

        }
    }
}
