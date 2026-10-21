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

@TargetClass(className = "org.apache.kafka.common.utils.AppInfoParser", onlyWith = RemoveJMXAccess.Enabled.class)
final class RemoveJMXAccess {
    static final class Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            if (!KafkaAppInfoAvailability.hasLegacyParser()) {
                return false;
            }
            int[] version = Arrays.stream(AppInfoParser.getVersion().split("\\."))
                    .filter(s -> s.matches("\\d+"))
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

@TargetClass(className = "org.apache.kafka.common.utils.AppInfoParser", onlyWith = RemoveJMXAccessPre42.Enabled.class)
final class RemoveJMXAccessPre42 {
    static final class Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            if (!KafkaAppInfoAvailability.hasLegacyParser()) {
                return false;
            }
            int[] version = Arrays.stream(AppInfoParser.getVersion().split("\\."))
                    .filter(s -> s.matches("\\d+"))
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


/**
 * The Kafka 4.4 client moved AppInfoParser and its nested AppInfo class into an
 * internal package. Use string-based native substitution targets to avoid
 * resolving a class literal that is absent in one of the supported versions.
 */
final class KafkaAppInfoAvailability {

    private static final String LEGACY = "org.apache.kafka.common.utils.AppInfoParser";
    private static final String RELOCATED = "org.apache.kafka.common.utils.internals.AppInfoParser";

    static boolean hasLegacyParser() {
        return isPresent(LEGACY);
    }

    static boolean hasRelocatedParser() {
        return isPresent(RELOCATED);
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, KafkaAppInfoAvailability.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

@TargetClass(className = "org.apache.kafka.common.utils.internals.AppInfoParser", innerClass = "AppInfo",
        onlyWith = RemoveJMXAccess44.Enabled.class)
final class RelocatedAppInfo {

    @Alias
    RelocatedAppInfo(long nowMs) {
    }
}

@TargetClass(className = "org.apache.kafka.common.utils.internals.AppInfoParser",
        onlyWith = RemoveJMXAccess44.Enabled.class)
final class RemoveJMXAccess44 {

    static final class Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return KafkaAppInfoAvailability.hasRelocatedParser();
        }
    }

    @Substitute
    public static synchronized void registerAppInfo(String prefix, String id, Metrics metrics, long nowMs) {
        registerMetrics(metrics, new RelocatedAppInfo(nowMs), id);
    }

    @Substitute
    public static synchronized void unregisterAppInfo(String prefix, String id, Metrics metrics) {
        unregisterMetrics(metrics, id);
    }

    @Alias
    private static native void registerMetrics(Metrics metrics, RelocatedAppInfo appInfo, String clientId);

    @Alias
    private static native void unregisterMetrics(Metrics metrics, String clientId);
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
