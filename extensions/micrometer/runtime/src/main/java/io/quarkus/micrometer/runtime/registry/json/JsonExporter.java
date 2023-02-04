/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.micrometer.runtime.registry.json;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;

public class JsonExporter {

    private static final Map<String, ?> JSON_CONFIG = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();

    public JsonExporter() {
    }

    public StringBuilder exportEverything(JsonMeterRegistry meterRegistry) {
        JsonObjectBuilder root = JSON_PROVIDER.createObjectBuilder();
        List<Gauge> gauges = new ArrayList<>();
        List<Counter> counters = new ArrayList<>();
        List<TimeGauge> timeGauges = new ArrayList<>();
        List<FunctionCounter> functionCounters = new ArrayList<>();
        List<Timer> timers = new ArrayList<>();
        List<LongTaskTimer> longTaskTimers = new ArrayList<>();
        List<FunctionTimer> functionTimers = new ArrayList<>();
        List<DistributionSummary> distributionSummaries = new ArrayList<>();
        List<Meter> meters = new ArrayList<>();
        meterRegistry.getMeters().forEach(meter -> meter.match(gauges::add,
                counters::add,
                timers::add,
                distributionSummaries::add,
                longTaskTimers::add,
                timeGauges::add,
                functionCounters::add,
                functionTimers::add,
                meters::add));
        exportCounters(counters).forEach(root::add);
        exportGauges(gauges).forEach(root::add);
        exportTimeGauges(timeGauges).forEach(root::add);
        exportFunctionCounters(functionCounters).forEach(root::add);
        exportTimers(timers).forEach(root::add);
        exportLongTaskTimers(longTaskTimers).forEach(root::add);
        exportFunctionTimers(functionTimers).forEach(root::add);
        exportDistributionSummaries(distributionSummaries).forEach(root::add);
        return stringify(root.build());
    }

    private Map<String, JsonValue> exportGauges(Collection<Gauge> gauges) {
        Map<String, JsonValue> result = new HashMap<String, JsonValue>(gauges.size());
        for (Gauge g : gauges) {
            double value = g.value();
            if (Double.isFinite(value)) {
                result.put(createExportKey(g.getId()), JSON_PROVIDER.createValue(value));
            }
        }
        return result;
    }

    private Map<String, JsonValue> exportTimeGauges(Collection<TimeGauge> timeGauges) {
        Map<String, JsonValue> result = new HashMap<String, JsonValue>(timeGauges.size());
        for (TimeGauge g : timeGauges) {
            double value = g.value();
            if (Double.isFinite(value)) {
                result.put(createExportKey(g.getId()), JSON_PROVIDER.createValue(value));
            }
        }
        return result;
    }

    private Map<String, JsonValue> exportCounters(Collection<Counter> counters) {
        return counters.stream()
                .collect(Collectors.toMap(counter -> createExportKey(counter.getId()),
                        counter -> JSON_PROVIDER.createValue(counter.count())));
    }

    private Map<String, JsonValue> exportFunctionCounters(Collection<FunctionCounter> counters) {
        return counters.stream()
                .collect(Collectors.toMap(counter -> createExportKey(counter.getId()),
                        counter -> JSON_PROVIDER.createValue(counter.count())));
    }

    private Map<String, JsonValue> exportTimers(Collection<Timer> timers) {
        Map<String, List<Timer>> groups = timers.stream().collect(Collectors.groupingBy(timer -> timer.getId().getName()));
        Map<String, JsonValue> result = new HashMap<>();
        for (Map.Entry<String, List<Timer>> group : groups.entrySet()) {
            JsonObjectBuilder builder = JSON_PROVIDER.createObjectBuilder();
            for (Timer timer : group.getValue()) {
                builder.add(createExportKey("count", timer.getId()), timer.count());
                builder.add(createExportKey("elapsedTime", timer.getId()), timer.totalTime(timer.baseTimeUnit()));
            }
            result.put(group.getKey(), builder.build());
        }
        return result;
    }

    private Map<String, JsonValue> exportLongTaskTimers(Collection<LongTaskTimer> timers) {
        Map<String, List<LongTaskTimer>> groups = timers.stream()
                .collect(Collectors.groupingBy(timer -> timer.getId().getName()));
        Map<String, JsonValue> result = new HashMap<>();
        for (Map.Entry<String, List<LongTaskTimer>> group : groups.entrySet()) {
            JsonObjectBuilder builder = JSON_PROVIDER.createObjectBuilder();
            for (LongTaskTimer timer : group.getValue()) {
                builder.add(createExportKey("activeTasks", timer.getId()), timer.activeTasks());
                builder.add(createExportKey("duration", timer.getId()), timer.duration(timer.baseTimeUnit()));
                builder.add(createExportKey("max", timer.getId()), timer.max(timer.baseTimeUnit()));
                builder.add(createExportKey("mean", timer.getId()), timer.mean(timer.baseTimeUnit()));
            }
            result.put(group.getKey(), builder.build());
        }
        return result;
    }

    private Map<String, JsonValue> exportFunctionTimers(Collection<FunctionTimer> timers) {
        Map<String, List<FunctionTimer>> groups = timers.stream()
                .collect(Collectors.groupingBy(timer -> timer.getId().getName()));
        Map<String, JsonValue> result = new HashMap<>();
        for (Map.Entry<String, List<FunctionTimer>> group : groups.entrySet()) {
            JsonObjectBuilder builder = JSON_PROVIDER.createObjectBuilder();
            for (FunctionTimer timer : group.getValue()) {
                builder.add(createExportKey("count", timer.getId()), timer.count());
                builder.add(createExportKey("elapsedTime", timer.getId()), timer.totalTime(timer.baseTimeUnit()));
            }
            result.put(group.getKey(), builder.build());
        }
        return result;
    }

    private Map<String, JsonValue> exportDistributionSummaries(Collection<DistributionSummary> distributionSummaries) {
        Map<String, List<DistributionSummary>> groups = distributionSummaries.stream()
                .collect(Collectors.groupingBy(summary -> summary.getId().getName()));
        Map<String, JsonValue> result = new HashMap<>();
        for (Map.Entry<String, List<DistributionSummary>> group : groups.entrySet()) {
            JsonObjectBuilder builder = JSON_PROVIDER.createObjectBuilder();
            for (DistributionSummary summary : group.getValue()) {
                HistogramSnapshot snapshot = summary.takeSnapshot();
                if (summary instanceof JsonDistributionSummary) {
                    double min = ((JsonDistributionSummary) summary).min();
                    // if there are no samples yet, show 0 as the min
                    builder.add(createExportKey("min", summary.getId()), !Double.isNaN(min) ? min : 0);
                }
                builder.add(createExportKey("count", summary.getId()), snapshot.count());
                builder.add(createExportKey("max", summary.getId()), snapshot.max());
                builder.add(createExportKey("mean", summary.getId()), snapshot.mean());
                for (ValueAtPercentile valueAtPercentile : snapshot.percentileValues()) {
                    if (Math.abs(valueAtPercentile.percentile() - 0.999) < 0.000001) {
                        builder.add(createExportKey("p999", summary.getId()),
                                valueAtPercentile.value());
                    } else {
                        builder.add(
                                createExportKey("p" + (int) Math.floor(valueAtPercentile.percentile() * 100), summary.getId()),
                                valueAtPercentile.value());
                    }
                }
            }
            result.put(group.getKey(), builder.build());
        }
        return result;
    }

    private StringBuilder stringify(JsonObject obj) {
        StringWriter out = new StringWriter();
        try (JsonWriter writer = JSON_PROVIDER.createWriterFactory(JSON_CONFIG).createWriter(out)) {
            writer.writeObject(obj);
        }
        return new StringBuilder(out.toString());
    }

    private String createExportKey(Meter.Id id) {
        return id.getName() + createTagsString(id.getTags());
    }

    private String createExportKey(String componentKey, Meter.Id id) {
        return componentKey + createTagsString(id.getTags());
    }

    private String createTagsString(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        } else {
            return ";" + tags.stream()
                    .map(tag -> tag.getKey() + "=" + tag.getValue()
                            .replace(";", "_"))
                    .collect(Collectors.joining(";"));
        }
    }

}
