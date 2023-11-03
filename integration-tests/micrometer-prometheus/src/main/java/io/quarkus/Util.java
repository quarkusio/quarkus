package io.quarkus;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;

public class Util {
    private Util() {
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
