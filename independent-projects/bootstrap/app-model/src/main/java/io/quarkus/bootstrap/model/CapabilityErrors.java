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
    private final Map<String, List<String>> missing = new HashMap<>();

    public void addConflict(String capability, String provider) {
        conflicts.computeIfAbsent(capability, k -> new ArrayList<>()).add(provider);
    }

    public void addMissing(String capability, String consumer) {
        missing.computeIfAbsent(capability, k -> new ArrayList<>()).add(consumer);
    }

    public boolean isEmpty() {
        return conflicts.isEmpty() && missing.isEmpty();
    }

    public String report() {
        final StringWriter sw = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(sw)) {
            reportProblems(conflicts, false, writer);
            reportProblems(missing, true, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate the capability error report out of conflicts " + conflicts
                    + " and unsatisfied " + missing, e);
        }
        return sw.getBuffer().toString();
    }

    private static void reportProblems(Map<String, List<String>> problems, boolean missing, BufferedWriter writer)
            throws IOException {
        if (problems.isEmpty()) {
            return;
        }
        if (missing) {
            writer.write(
                    "The following capability requirements aren't satisfied by the Quarkus extensions present on the classpath:");
        } else {
            writer.write("Please make sure there is only one provider of the following capabilities:");
        }
        // To have a consistent report over multiple builds, the capabilities and providers are ordered alphabetically
        final List<String> capabilities = new ArrayList<>(problems.size());
        capabilities.addAll(problems.keySet());
        Collections.sort(capabilities);
        for (String capability : capabilities) {
            writer.newLine();
            writer.append("capability ").append(capability).append(missing ? " is required by:" : " is provided by:");
            final List<String> extensions = problems.get(capability);
            Collections.sort(extensions);
            for (String extension : extensions) {
                writer.newLine();
                writer.append("  - ").append(extension);
            }
        }
        writer.newLine();
    }
}
