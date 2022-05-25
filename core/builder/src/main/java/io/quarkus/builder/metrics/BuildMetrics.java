package io.quarkus.builder.metrics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

import io.quarkus.builder.metrics.Json.JsonArrayBuilder;
import io.quarkus.builder.metrics.Json.JsonObjectBuilder;

public class BuildMetrics {

    static final Logger LOG = Logger.getLogger(BuildMetrics.class.getName());

    private volatile LocalDateTime started;
    private final String buildTargetName;
    private final ConcurrentMap<String, BuildStepRecord> records = new ConcurrentHashMap<>();
    private final AtomicInteger duplicates = new AtomicInteger();

    public BuildMetrics(String buildTargetName) {
        this.buildTargetName = buildTargetName;
    }

    public Collection<BuildStepRecord> getRecords() {
        return records.values();
    }

    public void buildStarted() {
        this.started = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void buildStepFinished(String stepId, String thread, LocalTime started, long duration) {
        BuildStepRecord prev = records.putIfAbsent(stepId, new BuildStepRecord(stepId, thread, started, duration));
        if (prev != null) {
            String newName = stepId + "_d#" + duplicates.incrementAndGet();
            LOG.debugf("A build step with the same identifier already exists - added a generated suffix: %s", newName);
            buildStepFinished(newName, thread, started, duration);
        }
    }

    public void dumpTo(Path file) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

        List<BuildStepRecord> sorted = new ArrayList<>(records.values());
        sorted.sort(new Comparator<BuildStepRecord>() {
            @Override
            public int compare(BuildStepRecord o1, BuildStepRecord o2) {
                return Long.compare(o2.duration, o1.duration);
            }
        });

        JsonObjectBuilder json = Json.object();
        json.put("buildTarget", buildTargetName);
        json.put("started", started.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        JsonArrayBuilder steps = Json.array();
        json.put("steps", steps);
        for (BuildStepRecord rec : sorted) {
            JsonObjectBuilder recJson = Json.object();
            recJson.put("stepId", rec.stepId);
            recJson.put("thread", rec.thread);
            recJson.put("started", rec.started.format(formatter));
            recJson.put("duration", rec.duration);
            steps.add(recJson);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile(), StandardCharsets.UTF_8))) {
            json.appendTo(writer);
        }
    }

    public static class BuildStepRecord {

        /**
         * The identifier of the build step.
         */
        public final String stepId;

        /**
         * The name of the thread this build step was executed on.
         */
        public final String thread;

        /**
         * The time the execution started.
         */
        public final LocalTime started;

        /**
         * The duration in ms.
         */
        public final long duration;

        BuildStepRecord(String stepId, String thread, LocalTime started, long duration) {
            this.stepId = stepId;
            this.thread = thread;
            this.started = started;
            this.duration = duration;
        }

    }

}
