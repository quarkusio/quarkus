package io.quarkus.extest.runtime.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public RuntimeValue<Map<String, Handler>> createNamed() {
        return new RuntimeValue<>(Map.of(TestNamedHandler.NAME, new TestNamedHandler()));
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

    public static class TestNamedHandler extends Handler {

        public static final String NAME = "TEST_NAMED";

        public final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
