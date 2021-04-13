package io.quarkus.micrometer.test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import io.micrometer.core.instrument.MeterRegistry;

public class Util {
    private Util() {
    }

    static void assertMessage(String attribute, List<LogRecord> records) {
        // look through log records and make sure there is a message about the specific attribute
        long i = records.stream().filter(x -> Arrays.stream(x.getParameters()).anyMatch(y -> y.equals(attribute)))
                .count();
        Assertions.assertEquals(1, i);
    }

    static String stackToString(Throwable t) {
        StringBuilder sb = new StringBuilder().append("\n");
        while (t.getCause() != null) {
            t = t.getCause();
        }
        sb.append(t.getClass()).append(": ").append(t.getMessage()).append("\n");
        Arrays.asList(t.getStackTrace()).forEach(x -> sb.append("\t").append(x.toString()).append("\n"));
        return sb.toString();
    }

    public static String foundServerRequests(MeterRegistry registry, String message) {
        return message + "\nFound:\n" + Util.listMeters(registry, "http.server.requests");
    }

    public static String foundClientRequests(MeterRegistry registry, String message) {
        return message + "\nFound:\n" + Util.listMeters(registry, "http.client.requests");
    }

    public static String listMeters(MeterRegistry registry, String meterName) {
        return registry.find(meterName).meters().stream()
                .map(x -> {
                    return x.getId().toString();
                })
                .collect(Collectors.joining("\n"));
    }

    public static String listMeters(MeterRegistry registry, String meterName, final String tag) {
        return registry.find(meterName).meters().stream()
                .map(x -> {
                    return x.getId().getTag(tag);
                })
                .collect(Collectors.joining(","));
    }
}
