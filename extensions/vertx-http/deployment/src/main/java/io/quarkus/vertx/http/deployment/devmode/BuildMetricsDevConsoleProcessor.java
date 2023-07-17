package io.quarkus.vertx.http.deployment.devmode;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.impl.LazyValue;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.vertx.http.deployment.devmode.BuildMetricsDevConsoleProcessor.DependecyGraph.Link;
import io.quarkus.vertx.http.deployment.devmode.BuildMetricsDevConsoleProcessor.DependecyGraph.Node;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Deprecated(forRemoval = true)
public class BuildMetricsDevConsoleProcessor {

    private static final Logger LOG = Logger.getLogger(BuildMetricsDevConsoleProcessor.class.getName());

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleTemplateInfoBuildItem collectMetrics(BuildSystemTargetBuildItem buildSystemTarget) {
        return new DevConsoleTemplateInfoBuildItem("buildMetrics",
                // We need to read the data lazily because the build is not finished yet at the time this build item is produced
                // This also means that no parsing is done until the build steps view is actually used
                new LazyValue<Map<String, Object>>(new Supplier<Map<String, Object>>() {

                    @Override
                    public Map<String, Object> get() {
                        Map<String, Object> metrics = new HashMap<>();
                        Map<String, JsonObject> stepIdToRecord = new HashMap<>();
                        Map<Integer, JsonObject> recordIdToRecord = new HashMap<>();
                        Map<String, List<JsonObject>> threadToRecords = new HashMap<>();
                        long buildDuration = 0;
                        LocalTime buildStarted = null;

                        Path metricsJsonFile = buildSystemTarget.getOutputDirectory().resolve("build-metrics.json");
                        if (Files.isReadable(metricsJsonFile)) {
                            try {
                                JsonObject data = new JsonObject(Files.readString(metricsJsonFile));
                                buildDuration = data.getLong("duration");
                                buildStarted = LocalDateTime
                                        .parse(data.getString("started"), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalTime();

                                JsonArray records = data.getJsonArray("records");
                                for (Object record : records) {
                                    JsonObject recordObj = (JsonObject) record;
                                    recordObj.put("encodedStepId", URLEncoder.encode(recordObj.getString("stepId"),
                                            StandardCharsets.UTF_8.toString()));
                                    String thread = recordObj.getString("thread");
                                    stepIdToRecord.put(recordObj.getString("stepId"), recordObj);
                                    recordIdToRecord.put(recordObj.getInteger("id"), recordObj);
                                    List<JsonObject> steps = threadToRecords.get(thread);
                                    if (steps == null) {
                                        steps = new ArrayList<>();
                                        threadToRecords.put(thread, steps);
                                    }
                                    steps.add(recordObj);
                                }

                                metrics.put("records", records);
                                metrics.put("items", data.getJsonArray("items"));
                                metrics.put("itemsCount", data.getInteger("itemsCount"));
                                metrics.put("duration", buildDuration);
                            } catch (IOException e) {
                                LOG.error(e);
                            }
                        }

                        // Build dependency graphs
                        Map<String, DependecyGraph> dependencyGraphs = new HashMap<>();
                        for (Map.Entry<String, JsonObject> e : stepIdToRecord.entrySet()) {
                            dependencyGraphs.put(e.getKey(),
                                    buildDependencyGraph(e.getValue(), stepIdToRecord, recordIdToRecord));
                        }
                        metrics.put("dependencyGraphs", dependencyGraphs);

                        // Time slots
                        long slotDuration = Math.max(10, buildDuration / 100);
                        List<Long> slots = new ArrayList<>();
                        long currentSlot = slotDuration;
                        while (currentSlot < buildDuration) {
                            slots.add(currentSlot);
                            currentSlot += slotDuration;
                        }
                        if (currentSlot != buildDuration) {
                            slots.add(buildDuration);
                        }
                        metrics.put("slots", slots);

                        Map<String, List<List<String>>> threadToSlotRecords = new HashMap<>();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

                        for (Map.Entry<String, List<JsonObject>> entry : threadToRecords.entrySet()) {
                            String thread = entry.getKey();
                            List<JsonObject> records = entry.getValue();
                            List<List<String>> threadSlots = new ArrayList<>();

                            for (Long slot : slots) {
                                List<String> slotRecords = new ArrayList<>();
                                for (JsonObject record : records) {
                                    LocalTime started = LocalTime.parse(record.getString("started"), formatter);
                                    long startAt = Duration.between(buildStarted, started).toMillis();
                                    if (startAt < slot && (slot - slotDuration) < (startAt + record.getLong("duration"))) {
                                        slotRecords.add(record.getString("stepId"));
                                    }
                                }
                                threadSlots.add(slotRecords);
                            }
                            threadToSlotRecords.put(thread, threadSlots);
                        }
                        metrics.put("threadSlotRecords", threadToSlotRecords);

                        return metrics;
                    }
                }));

    }

    DependecyGraph buildDependencyGraph(JsonObject step, Map<String, JsonObject> stepIdToRecord,
            Map<Integer, JsonObject> recordIdToRecord) {
        Set<Node> nodes = new HashSet<>();
        Set<Link> links = new HashSet<>();

        addNodesDependents(step, nodes, links, step, stepIdToRecord, recordIdToRecord);
        addNodeDependencies(step, nodes, links, step, stepIdToRecord, recordIdToRecord);
        return new DependecyGraph(nodes, links);
    }

    void addNodesDependents(JsonObject root, Set<Node> nodes, Set<Link> links, JsonObject record,
            Map<String, JsonObject> stepIdToRecord, Map<Integer, JsonObject> recordIdToRecord) {
        String stepId = record.getString("stepId");
        nodes.add(new Node(stepId, record.getString("encodedStepId")));
        for (Object dependentRecordId : record.getJsonArray("dependents")) {
            int recordId = (int) dependentRecordId;
            if (recordId != record.getInteger("id")) {
                JsonObject dependentRecord = recordIdToRecord.get(recordId);
                String dependentStepId = dependentRecord.getString("stepId");
                links.add(Link.dependent(root.equals(record), dependentStepId, stepId));
                nodes.add(new Node(dependentStepId, dependentRecord.getString("encodedStepId")));
                // NOTE: we do not fetch transient dependencies yet because the UI is not ready to show so many nodes
                // if (added) {
                // addNodesDependents(root, nodes, links, dependentRecord, stepIdToRecord, recordIdToRecord);
                // }
            }
        }
    }

    void addNodeDependencies(JsonObject root, Set<Node> nodes, Set<Link> links, JsonObject record,
            Map<String, JsonObject> stepIdToRecord, Map<Integer, JsonObject> recordIdToRecord) {
        for (Map.Entry<String, JsonObject> entry : stepIdToRecord.entrySet()) {
            for (Object dependentRecordId : entry.getValue().getJsonArray("dependents")) {
                int recordId = (int) dependentRecordId;
                if (record.getInteger("id") == recordId) {
                    links.add(Link.dependency(root.equals(record),
                            record.getString("stepId"), entry.getValue().getString("stepId")));
                    nodes.add(new Node(entry.getValue().getString("stepId"), entry.getValue().getString("encodedStepId")));
                    // NOTE: we do not fetch transient dependencies yet because the UI is not ready to show so many nodes
                    // if (added) {
                    // addNodeDependencies(root, nodes, links, entry.getValue(), stepIdToRecord, recordIdToRecord);
                    // }
                }
            }
        }
    }

    public static class DependecyGraph {

        public final Set<Node> nodes;
        public final Set<Link> links;

        public DependecyGraph(Set<Node> nodes, Set<Link> links) {
            this.nodes = nodes;
            this.links = links;
        }

        public static class Node {

            public final String stepId;
            public final String simpleName;
            public final String encodedStepId;

            public Node(String stepId, String encodedStepId) {
                this.stepId = stepId;
                this.encodedStepId = encodedStepId;
                int lastDot = stepId.lastIndexOf('.');
                String simple = lastDot > 0 ? stepId.substring(lastDot + 1) : stepId;
                int hash = simple.indexOf('#');
                if (hash > 0) {
                    StringBuilder sb = new StringBuilder();
                    char[] chars = simple.substring(0, hash).toCharArray();
                    for (char c : chars) {
                        if (Character.isUpperCase(c)) {
                            sb.append(c);
                        }
                    }
                    simple = sb + simple.substring(hash);
                }
                this.simpleName = simple;
            }

            @Override
            public int hashCode() {
                return Objects.hash(stepId);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                Node other = (Node) obj;
                return Objects.equals(stepId, other.stepId);
            }

        }

        public static class Link {

            static Link dependent(boolean direct, String source, String target) {
                return new Link(source, target, direct ? "directDependent" : "dependency");
            }

            static Link dependency(boolean direct, String source, String target) {
                return new Link(source, target, direct ? "directDependency" : "dependency");
            }

            public final String source;
            public final String target;
            public final String type;

            public Link(String source, String target, String type) {
                this.source = source;
                this.target = target;
                this.type = type;
            }

            @Override
            public int hashCode() {
                return Objects.hash(source, target);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                Link other = (Link) obj;
                return Objects.equals(source, other.source) && Objects.equals(target, other.target);
            }

        }

    }

}
