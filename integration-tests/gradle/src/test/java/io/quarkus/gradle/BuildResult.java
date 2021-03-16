package io.quarkus.gradle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildResult {

    public static final String SUCCESS_OUTCOME = "SUCCESS";
    public static final String UPTODATE_OUTCOME = "UP-TO-DATE";
    private static final String TASK_RESULT_PREFIX = "> Task";

    private Map<String, String> tasks;
    private String output;

    private BuildResult() {
    }

    public static BuildResult of(InputStream input) {
        BuildResult result = new BuildResult();
        final List<String> outputLines = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.toList());
        result.setTasks(outputLines.stream()
                .filter(l -> l.length() != 0 && l.startsWith(TASK_RESULT_PREFIX))
                .map(l -> l.replaceFirst(TASK_RESULT_PREFIX, "").trim())
                .map(l -> l.split(" "))
                .collect(Collectors.toMap(p -> p[0], p -> {
                    if (p.length == 2) {
                        return p[1];
                    }
                    return SUCCESS_OUTCOME;
                }, (v1, v2) -> v1)));
        result.setOutput(String.join("\n", outputLines));
        return result;
    }

    public void setTasks(Map<String, String> tasks) {
        this.tasks = tasks;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Map<String, String> getTasks() {
        return tasks;
    }

    public String getOutput() {
        return output;
    }
}
