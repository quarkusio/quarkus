package io.quarkus.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildResult {

    public static final String SUCCESS_OUTCOME = "SUCCESS";
    public static final String UPTODATE_OUTCOME = "UP-TO-DATE";
    public static final String FROM_CACHE = "FROM-CACHE";
    public static final String NO_SOURCE = "NO-SOURCE";
    private static final String TASK_RESULT_PREFIX = "> Task";

    private Map<String, String> tasks;
    private String output;

    private BuildResult() {
    }

    public static BuildResult of(File logFile) throws IOException {
        BuildResult result = new BuildResult();
        List<String> outputLines = Files.readAllLines(logFile.toPath());
        result.setTasks(outputLines.stream().filter(l -> l.startsWith(TASK_RESULT_PREFIX))
                .map(l -> l.replaceFirst(TASK_RESULT_PREFIX, "").trim()).map(l -> l.split(" "))
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

    public static boolean isSuccessful(String result) {
        return SUCCESS_OUTCOME.equals(result) || FROM_CACHE.equals(result) || UPTODATE_OUTCOME.equals(result)
                || NO_SOURCE.equals(result);
    }

    public Map<String, String> unsuccessfulTasks() {
        return tasks.entrySet().stream().filter(e -> !isSuccessful(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
