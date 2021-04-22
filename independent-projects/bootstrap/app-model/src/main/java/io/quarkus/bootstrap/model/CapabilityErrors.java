package io.quarkus.bootstrap.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CapabilityErrors {

    private final Map<String, List<String>> conflicts = new HashMap<>();

    public void addConflict(String capability, String provider) {
        conflicts.computeIfAbsent(capability, k -> new ArrayList<>()).add(provider);
    }

    public boolean isEmpty() {
        return conflicts.isEmpty();
    }

    public String report() {
        final StringWriter sw = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(sw)) {
            writer.write("Please make sure there is only one provider of the following capabilities:");
            // To have a consistent report over multiple builds, the capabilities and providers are ordered alphabetically
            final List<String> capabilities = new ArrayList<>(conflicts.size());
            capabilities.addAll(conflicts.keySet());
            Collections.sort(capabilities);
            for (String capability : capabilities) {
                writer.newLine();
                writer.append("capability ").append(capability).append(" is provided by:");
                final List<String> providers = conflicts.get(capability);
                Collections.sort(providers);
                for (String provider : providers) {
                    writer.newLine();
                    writer.append("  - ").append(provider);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate the capability conflicts report out of " + conflicts, e);
        }
        return sw.getBuffer().toString();
    }
}
