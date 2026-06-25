package io.quarkus.it.vertx.nativetransport;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class IoUringAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            return ConditionEvaluationResult.disabled("io_uring requires Linux");
        }

        try {
            Process process = new ProcessBuilder("uname", "-r").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String kernelVersion = reader.readLine();
                if (kernelVersion != null) {
                    String[] parts = kernelVersion.split("[.-]");
                    int major = Integer.parseInt(parts[0]);
                    int minor = Integer.parseInt(parts[1]);
                    if (major > 5 || (major == 5 && minor >= 6)) {
                        return ConditionEvaluationResult.enabled(
                                "Kernel " + kernelVersion + " supports io_uring");
                    }
                    return ConditionEvaluationResult.disabled(
                            "Kernel " + kernelVersion + " is too old for io_uring (requires 5.6+)");
                }
            }
        } catch (Exception e) {
            return ConditionEvaluationResult.disabled("Could not determine kernel version: " + e.getMessage());
        }
        return ConditionEvaluationResult.disabled("Could not determine kernel version");
    }
}
