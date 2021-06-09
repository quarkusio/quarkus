package io.quarkus.kafka.client.runtime.graal;

import static org.apache.kafka.common.record.CompressionType.GZIP;
import static org.apache.kafka.common.record.CompressionType.NONE;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.utils.AppInfoParser;
import org.graalvm.home.Version;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Here is where surgery happens
 * * Remove Snappy if not available (require GraalVM 21+).
 * * Remove JMX
 */

final class GraalVM20OrEarlier implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        return Version.getCurrent().compareTo(21) < 0;
    }
}

@TargetClass(value = CompressionType.class, onlyWith = GraalVM20OrEarlier.class)
final class SubstituteSnappy {

    @Substitute
    public static CompressionType forName(String name) {
        if (NONE.name.equals(name)) {
            return NONE;
        } else if (GZIP.name.equals(name)) {
            return GZIP;
        } else {
            throw new IllegalArgumentException("Unknown  or unsupported compression name: " + name);
        }
    }

    @Substitute
    public static CompressionType forId(int id) {
        switch (id) {
            case 0:
                return NONE;
            case 1:
                return GZIP;
            default:
                throw new IllegalArgumentException("Unknown or unsupported compression type id: " + id);
        }
    }
}

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
