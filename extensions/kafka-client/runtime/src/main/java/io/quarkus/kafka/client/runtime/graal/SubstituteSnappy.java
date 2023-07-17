package io.quarkus.kafka.client.runtime.graal;

import java.util.List;
import java.util.Map;

import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.utils.AppInfoParser;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Here is where surgery happens
 * * Remove JMX
 */

@TargetClass(value = AppInfoParser.class)
final class RemoveJMXAccess {

    @Substitute
    public static synchronized void registerAppInfo(String prefix, String id, Metrics metrics, long nowMs) {

    }

    @Substitute
    public static synchronized void unregisterAppInfo(String prefix, String id, Metrics metrics) {

    }

}

@TargetClass(value = JmxReporter.class)
final class JMXReporting {

    @Substitute
    public void reconfigure(Map<String, ?> configs) {

    }

    @Substitute
    public void init(List<KafkaMetric> metrics) {

    }

    @Substitute
    public void metricChange(KafkaMetric metric) {

    }

    @Substitute
    public void metricRemoval(KafkaMetric metric) {

    }

    @Substitute
    public void close() {
    }

}
