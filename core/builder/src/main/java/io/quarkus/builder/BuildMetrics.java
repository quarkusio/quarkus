package io.quarkus.builder;

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

import io.quarkus.builder.Json.JsonArrayBuilder;
import io.quarkus.builder.Json.JsonObjectBuilder;

public class BuildMetrics {

    static final Logger LOG = Logger.getLogger(BuildMetrics.class.getName());

    private volatile LocalDateTime started;
    private volatile long duration;
    private final String buildTargetName;
    private final ConcurrentMap<String, BuildStepRecord> records = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator;

    public BuildMetrics(String buildTargetName) {
        this.buildTargetName = buildTargetName;
        this.idGenerator = new AtomicInteger();
    }

    public Collection<BuildStepRecord> getRecords() {
        return records.values();
    }

    public void buildStarted() {
        this.started = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void buildFinished(long duration) {
        this.duration = duration;
    }

    public void buildStepFinished(StepInfo stepInfo, String thread, LocalTime started, long duration) {
        records.put(stepInfo.getBuildStep().getId(),
                new BuildStepRecord(idGenerator.incrementAndGet(), stepInfo, thread, started, duration));
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
        json.put("duration", duration);

        JsonArrayBuilder steps = Json.array();
        json.put("records", steps);
        for (BuildStepRecord rec : sorted) {
            JsonObjectBuilder recObject = Json.object();
            recObject.put("id", rec.id);
            recObject.put("stepId", rec.stepInfo.getBuildStep().getId());
            recObject.put("thread", rec.thread);
            recObject.put("started", rec.started.format(formatter));
            recObject.put("duration", rec.duration);
            JsonArrayBuilder dependentsArray = Json.array();
            for (StepInfo dependent : rec.stepInfo.getDependents()) {
                BuildStepRecord dependentRecord = records.get(dependent.getBuildStep().getId());
                if (dependentRecord != null) {
                    dependentsArray.add(dependentRecord.id);
                } else {
                    LOG.warnf("Dependent record not found for stepId: %s", dependent.getBuildStep().getId());
                }
            }
            recObject.put("dependents", dependentsArray);
            steps.add(recObject);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile(), StandardCharsets.UTF_8))) {
            json.appendTo(writer);
        }
    }

    public static class BuildStepRecord {

        /**
         * A unique record id.
         */
        public final int id;

        public final StepInfo stepInfo;

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

        BuildStepRecord(int id, StepInfo stepInfo, String thread, LocalTime started, long duration) {
            this.id = id;
            this.stepInfo = stepInfo;
            this.thread = thread;
            this.started = started;
            this.duration = duration;
        }

    }

}
