package io.quarkus.micrometer.runtime.dev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@ApplicationScoped
public class MicrometerJsonRpcService {

    @Inject
    MeterRegistry registry;

    public List<Map<String, Object>> getMetrics() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Meter meter : registry.getMeters()) {
            Map<String, Object> meterInfo = new HashMap<>();
            meterInfo.put("name", meter.getId().getName());
            meterInfo.put("type", meter.getId().getType().name());
            meterInfo.put("description", meter.getId().getDescription());

            // Get tags
            List<Map<String, String>> tags = new ArrayList<>();
            meter.getId().getTags().forEach(tag -> {
                Map<String, String> tagMap = new HashMap<>();
                tagMap.put("key", tag.getKey());
                tagMap.put("value", tag.getValue());
                tags.add(tagMap);
            });
            meterInfo.put("tags", tags);

            // Get value based on meter type (filter out NaN/Infinity as they are not valid JSON numbers)
            if (meter instanceof Counter) {
                double count = ((Counter) meter).count();
                if (Double.isFinite(count)) {
                    meterInfo.put("value", count);
                }
            } else if (meter instanceof Gauge) {
                double value = ((Gauge) meter).value();
                if (Double.isFinite(value)) {
                    meterInfo.put("value", value);
                }
            } else if (meter instanceof Timer) {
                Timer timer = (Timer) meter;
                meterInfo.put("count", timer.count());
                meterInfo.put("totalTime", finiteOrZero(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)));
                meterInfo.put("mean", finiteOrZero(timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)));
                meterInfo.put("max", finiteOrZero(timer.max(java.util.concurrent.TimeUnit.MILLISECONDS)));
            }

            result.add(meterInfo);
        }

        return result;
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();

        int counters = 0;
        int gauges = 0;
        int timers = 0;
        int other = 0;

        for (Meter meter : registry.getMeters()) {
            switch (meter.getId().getType()) {
                case COUNTER:
                    counters++;
                    break;
                case GAUGE:
                    gauges++;
                    break;
                case TIMER:
                case LONG_TASK_TIMER:
                    timers++;
                    break;
                default:
                    other++;
                    break;
            }
        }

        summary.put("total", registry.getMeters().size());
        summary.put("counters", counters);
        summary.put("gauges", gauges);
        summary.put("timers", timers);
        summary.put("other", other);

        return summary;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
