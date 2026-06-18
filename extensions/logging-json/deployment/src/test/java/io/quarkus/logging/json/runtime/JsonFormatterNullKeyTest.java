package io.quarkus.logging.json.runtime;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

class JsonFormatterNullKeyTest {

    @Test
    void jsonLogGeneratorAcceptsNullKeysWhenExclusionsAreConfigured() throws Exception {
        JsonFormatter.JsonLogGenerator generator = newJsonLogGenerator(Set.of("timestamp", "sequence"));

        assertThatCode(() -> {
            generator.add(null, true);
            generator.add(null, 1);
            generator.add(null, 1L);
            generator.add(null, Map.of("key", "value"));
            generator.add(null, "value");
            generator.startObject(null).endObject();
            generator.startArray(null).endArray();
        }).doesNotThrowAnyException();
    }

    @Test
    void formatterKeepsExceptionRecordsWhenExclusionsAreConfigured() {
        JsonFormatter formatter = new JsonFormatter(null, Set.of("timestamp", "sequence"), Map.of());

        LogRecord record = new LogRecord(Level.WARNING, "Something went wrong");
        record.setThrown(new RuntimeException("boom"));

        assertThatCode(() -> formatter.format(record)).doesNotThrowAnyException();
    }

    private static JsonFormatter.JsonLogGenerator newJsonLogGenerator(Set<String> excludedKeys) throws Exception {
        Class<?> generatorClass = Class.forName("org.jboss.logmanager.formatters.StructuredFormatter$Generator");
        Object delegate = Proxy.newProxyInstance(
                generatorClass.getClassLoader(),
                new Class<?>[] { generatorClass },
                (proxy, method, args) -> {
                    if (method.getReturnType().equals(generatorClass)) {
                        return proxy;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return null;
                });
        Constructor<JsonFormatter.JsonLogGenerator> constructor = JsonFormatter.JsonLogGenerator.class
                .getDeclaredConstructor(generatorClass, Set.class);
        constructor.setAccessible(true);
        return constructor.newInstance(delegate, excludedKeys);
    }
}
