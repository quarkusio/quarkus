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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.builder.item.BuildItem;

public class BuildMetrics {

    public static final String BUILDER_METRICS_ENABLED = "quarkus.builder.metrics.enabled";
    public static final String BUILDER_METRICS_EXTENDED_CAPTURE = "quarkus.builder.metrics.extended-capture";

    static final Logger LOG = Logger.getLogger(BuildMetrics.class.getName());

    private volatile LocalDateTime started;
    private volatile long duration;
    private final String buildTargetName;
    // build step id -> record
    private final ConcurrentMap<String, BuildStepRecord> records;
    // build item class -> count
    private final ConcurrentMap<String, Long> buildItems;
    // build step id -> produced build items
    private final ConcurrentMap<String, List<String>> buildItemsExtended;
    private final AtomicInteger idGenerator;

    public BuildMetrics(String buildTargetName) {
        boolean enabled = Boolean.getBoolean(BUILDER_METRICS_ENABLED)
                // This system property is deprecated and will be removed
                || Boolean.getBoolean("quarkus.debug.dump-build-metrics");
        this.buildTargetName = buildTargetName;
        if (enabled) {
            this.idGenerator = new AtomicInteger();
            this.records = new ConcurrentHashMap<>();
            if (Boolean.getBoolean(BUILDER_METRICS_EXTENDED_CAPTURE)) {
                this.buildItemsExtended = new ConcurrentHashMap<>();
                this.buildItems = null;
            } else {
                this.buildItemsExtended = null;
                this.buildItems = new ConcurrentHashMap<>();
            }
        } else {
            this.idGenerator = null;
            this.records = null;
            this.buildItemsExtended = null;
            this.buildItems = null;
        }
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
        if (enabled()) {
            records.put(stepInfo.getBuildStep().getId(),
                    new BuildStepRecord(idGenerator.incrementAndGet(), stepInfo, thread, started, duration));
        }
    }

    public void buildItemProduced(StepInfo stepInfo, BuildItem buildItem) {
        if (enabled()) {
            if (buildItems != null) {
                buildItems.compute(buildItem.getClass().getName(), this::itemProduced);
            } else {
                buildItemsExtended.compute(stepInfo.getBuildStep().getId(), (key, list) -> {
                    String buildItemClass = buildItem.getClass().getName();
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    list.add(buildItemClass);
                    return list;
                });
            }
        }
    }

    private Long itemProduced(String key, Long val) {
        return val == null ? 1 : val + 1;
    }

    public void dumpTo(Path file) throws IOException {
        if (enabled()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

            List<BuildStepRecord> sortedSteps = new ArrayList<>(records.values());
            sortedSteps.sort(new Comparator<BuildStepRecord>() {
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
            for (BuildStepRecord rec : sortedSteps) {
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
                if (buildItemsExtended != null) {
                    List<String> items = buildItemsExtended.get(rec.stepInfo.getBuildStep().getId());
                    if (items != null) {
                        JsonArrayBuilder producedItems = Json.array();
                        // build item class -> count
                        Map<String, Long> counts = items
                                .stream()
                                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                        List<Entry<String, Long>> sortedItems = new ArrayList<>(counts.entrySet());
                        sortedItems.sort(this::compareBuildItems);
                        for (Entry<String, Long> e : sortedItems) {
                            producedItems.add(Json.object()
                                    .put("item", e.getKey())
                                    .put("count", e.getValue().longValue()));
                        }
                        recObject.put("producedItems", producedItems);
                    }
                }
                steps.add(recObject);
            }

            List<Entry<String, Long>> sortedItems;
            if (buildItemsExtended != null) {
                // build item class -> count
                Map<String, Long> counts = buildItemsExtended.values()
                        .stream()
                        .flatMap(List::stream)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                sortedItems = new ArrayList<>(counts.entrySet());
            } else {
                sortedItems = new ArrayList<>(buildItems.size());
                buildItems.entrySet().forEach(sortedItems::add);
            }
            sortedItems.sort(this::compareBuildItems);
            JsonArrayBuilder items = Json.array();
            json.put("items", items);
            long itemsCount = 0;
            for (Entry<String, Long> e : sortedItems) {
                JsonObjectBuilder itemObject = Json.object();
                itemObject.put("class", e.getKey());
                itemObject.put("count", e.getValue());
                items.add(itemObject);
                itemsCount += e.getValue();
            }
            json.put("itemsCount", itemsCount);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile(), StandardCharsets.UTF_8))) {
                json.appendTo(writer);
            }
        }
    }

    private boolean enabled() {
        return records != null;
    }

    private int compareBuildItems(Entry<String, Long> o1, Entry<String, Long> o2) {
        return Long.compare(o2.getValue(), o1.getValue());
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
