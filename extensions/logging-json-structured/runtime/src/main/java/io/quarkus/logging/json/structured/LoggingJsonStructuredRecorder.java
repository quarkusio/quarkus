package io.quarkus.logging.json.structured;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Formatter;

import io.quarkus.logging.json.structured.providers.ArgumentsJsonProvider;
import io.quarkus.logging.json.structured.providers.HostNameJsonProvider;
import io.quarkus.logging.json.structured.providers.LogLevelJsonProvider;
import io.quarkus.logging.json.structured.providers.LoggerClassNameJsonProvider;
import io.quarkus.logging.json.structured.providers.LoggerNameJsonProvider;
import io.quarkus.logging.json.structured.providers.MDCJsonProvider;
import io.quarkus.logging.json.structured.providers.MessageJsonProvider;
import io.quarkus.logging.json.structured.providers.NDCJsonProvider;
import io.quarkus.logging.json.structured.providers.ProcessIdJsonProvider;
import io.quarkus.logging.json.structured.providers.ProcessNameJsonProvider;
import io.quarkus.logging.json.structured.providers.SequenceJsonProvider;
import io.quarkus.logging.json.structured.providers.StackTraceJsonProvider;
import io.quarkus.logging.json.structured.providers.ThreadIDJsonProvider;
import io.quarkus.logging.json.structured.providers.ThreadNameJsonProvider;
import io.quarkus.logging.json.structured.providers.TimestampJsonProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LoggingJsonStructuredRecorder {
    public RuntimeValue<Optional<Formatter>> initializeJsonLogging(JsonStructuredConfig config) {
        if (!config.enable) {
            return new RuntimeValue<>(Optional.empty());
        }
        List<JsonProvider> providers = new ArrayList<>();
        providers.add(new TimestampJsonProvider(config.dateFormat));
        providers.add(new SequenceJsonProvider());
        providers.add(new LoggerClassNameJsonProvider());
        providers.add(new LoggerNameJsonProvider());
        providers.add(new LogLevelJsonProvider());
        providers.add(new MessageJsonProvider());
        providers.add(new ThreadNameJsonProvider());
        providers.add(new ThreadIDJsonProvider());
        providers.add(new MDCJsonProvider());
        providers.add(new NDCJsonProvider());
        providers.add(new HostNameJsonProvider());
        providers.add(new ProcessNameJsonProvider());
        providers.add(new ProcessIdJsonProvider());
        providers.add(new StackTraceJsonProvider());
        providers.add(new ArgumentsJsonProvider(config));

        return new RuntimeValue<>(Optional.of(new JsonFormatter(providers, config)));
    }
}
