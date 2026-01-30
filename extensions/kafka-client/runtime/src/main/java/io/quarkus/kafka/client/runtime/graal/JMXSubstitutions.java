package io.quarkus.kafka.client.runtime.graal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.utils.AppInfoParser;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Here is where surgery happens
 * * Remove JMX
 */

@TargetClass(value = AppInfoParser.class, onlyWith = RemoveJMXAccess.Enabled.class)
final class RemoveJMXAccess {
    static final class Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            int[] version = Arrays.stream(AppInfoParser.getVersion().split("\\."))
                    .mapToInt(Integer::parseInt)
                    .toArray();
            return version[0] > 4 || (version[0] == 4 && version[1] >= 2);
        }
    }

    @Substitute
    public static synchronized void registerAppInfo(String prefix, String id, Metrics metrics, long nowMs) {
        registerMetrics(metrics, new AppInfoParser.AppInfo(nowMs), id);
    }

    @Substitute
    public static synchronized void unregisterAppInfo(String prefix, String id, Metrics metrics) {
        unregisterMetrics(metrics, id);
    }

    @Alias
    private static native void registerMetrics(Metrics metrics, AppInfoParser.AppInfo appInfo, String clientId);

    @Alias
    private static native void unregisterMetrics(Metrics metrics, String clientId);

}

@TargetClass(value = AppInfoParser.class, onlyWith = RemoveJMXAccessPre42.Enabled.class)
final class RemoveJMXAccessPre42 {
    static final class Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            int[] version = Arrays.stream(AppInfoParser.getVersion().split("\\."))
                    .mapToInt(Integer::parseInt)
                    .toArray();
            return version[0] < 4 || (version[0] == 4 && version[1] < 2);
        }
    }

    @Substitute
    public static synchronized void registerAppInfo(String prefix, String id, Metrics metrics, long nowMs) {
        registerMetrics(metrics, new AppInfoParser.AppInfo(nowMs));
    }

    @Substitute
    public static synchronized void unregisterAppInfo(String prefix, String id, Metrics metrics) {
        unregisterMetrics(metrics);
    }

    @Alias
    private static native void registerMetrics(Metrics metrics, AppInfoParser.AppInfo appInfo);

    @Alias
    private static native void unregisterMetrics(Metrics metrics);

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
