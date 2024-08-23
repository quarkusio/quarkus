package io.quarkus.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate(basePath = "")
public class BuildReport {

    static native TemplateInstance buildReport(String buildTarget, long duration, Set<String> threads,
            List<BuildStepRecord> records, List<BuildItem> buildItems, int buildItemsCount);

    private final BuildSystemRunner runner;

    public BuildReport(BuildSystemRunner runner) {
        this.runner = runner;
    }

    File generate() throws IOException {
        File metricsJsonFile = runner.getProjectRoot().resolve(runner.getBuildTool().getBuildDirectory())
                .resolve("build-metrics.json").toFile();
        if (!metricsJsonFile.canRead()) {
            throw new IllegalStateException("Build metrics file cannot be read: " + metricsJsonFile);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(metricsJsonFile);

        String buildTarget = root.get("buildTarget").asText();
        long duration = root.get("duration").asLong();
        Set<String> threads = new HashSet<>();
        List<BuildStepRecord> records = new ArrayList<>();
        List<BuildItem> items = new ArrayList<>();
        int itemsCount = root.get("itemsCount").asInt();

        for (JsonNode record : root.get("records")) {
            String thread = record.get("thread").asText();
            threads.add(thread);
            records.add(new BuildStepRecord(record.get("stepId").asText(), record.get("started").asText(),
                    record.get("duration").asLong(), thread));
        }

        for (JsonNode item : root.get("items")) {
            items.add(new BuildItem(item.get("class").asText(), item.get("count").asInt()));
        }

        File output = runner.getProjectRoot().resolve(runner.getBuildTool().getBuildDirectory())
                .resolve("build-report.html").toFile();
        if (output.exists() && !output.canWrite()) {
            throw new IllegalStateException("Build report file cannot be written to: " + output);
        }
        try {
            Files.writeString(output.toPath(),
                    BuildReport.buildReport(buildTarget, duration, threads, records, items, itemsCount).render());
            return output;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static class BuildStepRecord {

        public final String stepId;
        public final String started;
        public final long duration;
        public final String thread;

        public BuildStepRecord(String stepId, String started, long duration, String thread) {
            this.stepId = stepId;
            this.started = started;
            this.duration = duration;
            this.thread = thread;
        }

    }

    public static class BuildItem {

        public final String clazz;
        public final int count;

        public BuildItem(String clazz, int count) {
            this.clazz = clazz;
            this.count = count;
        }

    }

}
