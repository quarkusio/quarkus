package io.smallrye.reactive.kafka.graal;

import static org.apache.kafka.common.record.CompressionType.GZIP;
import static org.apache.kafka.common.record.CompressionType.NONE;

import java.lang.invoke.MethodHandle;
import java.util.List;

import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.utils.AppInfoParser;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Here is where surgery happens
 * * Remove Snappy
 * * Remove JMX
 */

@TargetClass(value = CompressionType.class, innerClass = "SnappyConstructors")
final class SubstituteSnappy {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    static MethodHandle INPUT = null;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    static MethodHandle OUTPUT = null;

}

@TargetClass(value = CompressionType.class)
final class FixEnumAccess {

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
    public static synchronized void registerAppInfo(String prefix, String id, Metrics metrics) {

    }

    @Substitute
    public static synchronized void unregisterAppInfo(String prefix, String id, Metrics metrics) {

    }

}

@TargetClass(value = JmxReporter.class)
final class JMXReporting {

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
