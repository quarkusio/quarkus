/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.jaeger.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.spi.MetricsFactory;

// TODO: Determine if there is a more automated way we can configure the metrics
// from the annotations.
@TargetClass(className = "io.jaegertracing.internal.metrics.Metrics")
public final class Target_Metrics {

    @Alias
    public Counter traceStartedSampled;

    @Alias
    public Counter traceStartedNotSampled;

    @Alias
    public Counter tracesJoinedSampled;

    @Alias
    public Counter tracesJoinedNotSampled;

    @Alias
    public Counter spansStartedSampled;

    @Alias
    public Counter spansStartedNotSampled;

    @Alias
    public Counter spansFinished;

    @Alias
    public Counter decodingErrors;

    @Alias
    public Counter reporterSuccess;

    @Alias
    public Counter reporterFailure;

    @Alias
    public Counter reporterDropped;

    @Alias
    public Gauge reporterQueueLength;

    @Alias
    public Counter samplerRetrieved;

    @Alias
    public Counter samplerQueryFailure;

    @Alias
    public Counter samplerUpdated;

    @Alias
    public Counter samplerParsingFailure;

    @Alias
    public Counter baggageUpdateSuccess;

    @Alias
    public Counter baggageUpdateFailure;

    @Alias
    public Counter baggageTruncate;

    @Alias
    public Counter baggageRestrictionsUpdateSuccess;

    @Alias
    public Counter baggageRestrictionsUpdateFailure;

    @Substitute
    private void createMetrics(MetricsFactory factory, String metricsPrefix) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("state", "started");
        tags.put("sampled", "y");
        traceStartedSampled = factory.createCounter(metricsPrefix + "traces", tags);

        tags = new HashMap<String, String>();
        tags.put("state", "started");
        tags.put("sampled", "n");
        traceStartedNotSampled = factory.createCounter(metricsPrefix + "traces", tags);

        tags = new HashMap<String, String>();
        tags.put("state", "joined");
        tags.put("sampled", "y");
        tracesJoinedSampled = factory.createCounter(metricsPrefix + "traces", tags);

        tags = new HashMap<String, String>();
        tags.put("state", "joined");
        tags.put("sampled", "n");
        tracesJoinedNotSampled = factory.createCounter(metricsPrefix + "traces", tags);

        tags = new HashMap<String, String>();
        tags.put("sampled", "y");
        spansStartedSampled = factory.createCounter(metricsPrefix + "started_spans", tags);

        tags = new HashMap<String, String>();
        tags.put("sampled", "n");
        spansStartedNotSampled = factory.createCounter(metricsPrefix + "started_spans", tags);

        spansFinished = factory.createCounter(metricsPrefix + "finished_spans", Collections.emptyMap());

        decodingErrors = factory.createCounter(metricsPrefix + "span_context_decoding_errors", Collections.emptyMap());

        tags = new HashMap<String, String>();
        tags.put("result", "ok");
        reporterSuccess = factory.createCounter(metricsPrefix + "reporter_spans", tags);

        tags = new HashMap<String, String>();
        tags.put("result", "err");
        reporterFailure = factory.createCounter(metricsPrefix + "reporter_spans", tags);

        tags = new HashMap<String, String>();
        tags.put("result", "dropped");
        reporterDropped = factory.createCounter(metricsPrefix + "reporter_spans", tags);

        reporterQueueLength = factory.createGauge(metricsPrefix + "reporter_queue_length", Collections.emptyMap());

        tags = new HashMap<String, String>();
        tags.put("result", "ok");
        samplerRetrieved = factory.createCounter(metricsPrefix + "sampler_queries", tags);

        tags = new HashMap<String, String>();
        tags.put("result", "err");
        samplerQueryFailure = factory.createCounter(metricsPrefix + "sampler_queries", tags);

        tags = new HashMap<String, String>();
        tags.put("result", "ok");
        samplerUpdated = factory.createCounter(metricsPrefix + "sampler_updates", tags);

        tags = new HashMap<String, String>();
        tags.put("result", "err");
        samplerParsingFailure = factory.createCounter(metricsPrefix + "sampler_updates", tags);

        tags = new HashMap<String, String>();
        tags.put("result", "ok");
        baggageUpdateSuccess = factory.createCounter(metricsPrefix + "baggage_updates", tags);

        tags = new HashMap<String, String>();
        tags.put("result", "err");
        baggageUpdateFailure = factory.createCounter(metricsPrefix + "baggage_updates", tags);

        baggageTruncate = factory.createCounter(metricsPrefix + "baggage_truncations", Collections.emptyMap());

        tags = new HashMap<String, String>();
        tags.put("result", "ok");
        baggageRestrictionsUpdateSuccess = factory.createCounter(metricsPrefix + "baggage_restrictions_updates", tags);

        tags = new HashMap<String, String>();
        tags.put("result", "err");
        baggageRestrictionsUpdateFailure = factory.createCounter(metricsPrefix + "baggage_restrictions_updates", tags);
    }
}
