package io.quarkus.smallrye.metrics.runtime;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

import io.smallrye.metrics.ExtendedMetadata;
import io.smallrye.metrics.MetricRegistries;

/**
 * Mimics GC metrics from Micrometer. Most of the logic here is basically copied from
 * {@link <a href=
 * "https://github.com/micrometer-metrics/micrometer/tree/master/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/jvm">Micrometer
 * JVM metrics</a>}.
 */
class MicrometerGCMetrics {

    MicrometerGCMetrics() {
        for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
            String name = mbean.getName();
            if (isYoungGenPool(name)) {
                youngGenPoolName = name;
            } else if (isOldGenPool(name)) {
                oldGenPoolName = name;
            }
        }
    }

    private String youngGenPoolName;
    private String oldGenPoolName;

    // jvm.gc.live.data.size metric
    private AtomicLong liveDataSize = new AtomicLong(0);
    // jvm.gc.max.data.size metric
    private AtomicLong maxDataSize = new AtomicLong(0);
    // jvm.gc.memory.promoted metric
    private AtomicLong promotedBytes = new AtomicLong(0);
    // jvm.gc.memory.allocated metric
    private AtomicLong allocatedBytes = new AtomicLong(0);

    // Mimicking the jvm.gc.pause timer. We don't have an exact equivalent of Micrometer's timer, so emulate
    // it with one gauge and two counters.
    // We use a wrapper class to wrap the 'cause' and 'action' fields of GC event descriptors into one class
    // We defer registering these metrics to runtime, because we don't assume we know in advance the full set of causes and actions

    static class CauseAndActionWrapper {
        private String cause;
        private String action;

        public CauseAndActionWrapper(String cause, String action) {
            this.cause = cause;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CauseAndActionWrapper that = (CauseAndActionWrapper) o;
            return Objects.equals(cause, that.cause) &&
                    Objects.equals(action, that.action);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cause, action);
        }
    }

    // keeps track of maximum gc pause lengths for a given GC cause and action
    private Map<CauseAndActionWrapper, AtomicLong> gcPauseMax = new HashMap<>();

    // and the same for concurrent GC phases
    private Map<CauseAndActionWrapper, AtomicLong> gcPauseMaxConcurrent = new HashMap<>();

    // To keep track of notification listeners that we register so we can clean them up later
    private Map<NotificationEmitter, NotificationListener> notificationEmitters = new HashMap<>();

    public Long getLiveDataSize() {
        return liveDataSize.get();
    }

    public Long getMaxDataSize() {
        return maxDataSize.get();
    }

    public Long getPromotedBytes() {
        return promotedBytes.get();
    }

    public Long getAllocatedBytes() {
        return allocatedBytes.get();
    }

    public void startWatchingNotifications() {
        final AtomicLong youngGenSizeAfter = new AtomicLong(0L);
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (!(mbean instanceof NotificationEmitter)) {
                continue;
            }
            NotificationListener notificationListener = (notification, ref) -> {
                if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                    return;
                }
                CompositeData cd = (CompositeData) notification.getUserData();
                GarbageCollectionNotificationInfo notificationInfo = GarbageCollectionNotificationInfo.from(cd);

                String gcCause = notificationInfo.getGcCause();
                String gcAction = notificationInfo.getGcAction();
                GcInfo gcInfo = notificationInfo.getGcInfo();
                long duration = gcInfo.getDuration();

                MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
                String metricName = isConcurrentPhase(gcCause) ? "jvm.gc.concurrent.phase.time" : "jvm.gc.pause";
                Map<CauseAndActionWrapper, AtomicLong> mapForStoringMax = isConcurrentPhase(gcCause) ? gcPauseMax
                        : gcPauseMaxConcurrent;

                Tag[] tags = new Tag[] { new Tag("action", gcAction), new Tag("cause", gcCause) };
                CauseAndActionWrapper causeAndAction = new CauseAndActionWrapper(gcCause, gcAction);

                MetricID pauseSecondsMaxMetricID = new MetricID(metricName + ".seconds.max", tags);
                AtomicLong gcPauseMaxValue = mapForStoringMax.computeIfAbsent(causeAndAction, (k) -> new AtomicLong(0));
                if (duration > gcPauseMaxValue.get()) {
                    gcPauseMaxValue.set(duration); // update the maximum GC length if needed
                }
                if (!registry.getGauges().containsKey(pauseSecondsMaxMetricID)) {
                    registry.register(new ExtendedMetadata(metricName + ".seconds.max",
                            MetricType.GAUGE,
                            MetricUnits.NONE,
                            "Time spent in GC pause",
                            true),
                            new LambdaGauge(() -> mapForStoringMax.get(causeAndAction).doubleValue() / 1000.0), tags);
                }

                ExtendedMetadata countMetadata = new ExtendedMetadata(metricName + ".seconds.count",
                        MetricType.COUNTER,
                        MetricUnits.NONE,
                        "Time spent in GC pause",
                        true,
                        metricName.replace(".", "_") + "_seconds_count");
                registry.counter(countMetadata, tags).inc();

                registry.counter(new ExtendedMetadata(metricName + ".seconds.sum",
                        MetricType.COUNTER,
                        MetricUnits.MILLISECONDS,
                        "Time spent in GC pause",
                        true,
                        metricName.replace(".", "_") + "_seconds_sum"), tags).inc(duration);

                // Update promotion and allocation counters
                final Map<String, MemoryUsage> before = gcInfo.getMemoryUsageBeforeGc();
                final Map<String, MemoryUsage> after = gcInfo.getMemoryUsageAfterGc();

                if (oldGenPoolName != null) {
                    final long oldBefore = before.get(oldGenPoolName).getUsed();
                    final long oldAfter = after.get(oldGenPoolName).getUsed();
                    final long delta = oldAfter - oldBefore;
                    if (delta > 0L) {
                        promotedBytes.addAndGet(delta);
                    }

                    // Some GC implementations such as G1 can reduce the old gen size as part of a minor GC. To track the
                    // live data size we record the value if we see a reduction in the old gen heap size or
                    // after a major GC.
                    if (oldAfter < oldBefore || GcGenerationAge.fromName(notificationInfo.getGcName()) == GcGenerationAge.OLD) {
                        liveDataSize.set(oldAfter);
                        final long oldMaxAfter = after.get(oldGenPoolName).getMax();
                        maxDataSize.set(oldMaxAfter);
                    }
                }

                if (youngGenPoolName != null) {
                    final long youngBefore = before.get(youngGenPoolName).getUsed();
                    final long youngAfter = after.get(youngGenPoolName).getUsed();
                    final long delta = youngBefore - youngGenSizeAfter.get();
                    youngGenSizeAfter.set(youngAfter);
                    if (delta > 0L) {
                        allocatedBytes.addAndGet(delta);
                    }
                }
            };
            NotificationEmitter notificationEmitter = (NotificationEmitter) mbean;
            notificationEmitter.addNotificationListener(notificationListener, null, null);
            notificationEmitters.put(notificationEmitter, notificationListener);
        }
    }

    public void cleanUp() {
        notificationEmitters.forEach((emitter, listener) -> {
            try {
                emitter.removeNotificationListener(listener);
            } catch (ListenerNotFoundException e) {
            }
        });
    }

    private boolean isYoungGenPool(String name) {
        return name.endsWith("Eden Space");
    }

    private boolean isOldGenPool(String name) {
        return name.endsWith("Old Gen") || name.endsWith("Tenured Gen");
    }

    private boolean isConcurrentPhase(String cause) {
        return "No GC".equals(cause);
    }

    enum GcGenerationAge {
        OLD,
        YOUNG,
        UNKNOWN;

        private static Map<String, GcGenerationAge> knownCollectors = new HashMap<String, GcGenerationAge>() {
            {
                put("ConcurrentMarkSweep", OLD);
                put("Copy", YOUNG);
                put("G1 Old Generation", OLD);
                put("G1 Young Generation", YOUNG);
                put("MarkSweepCompact", OLD);
                put("PS MarkSweep", OLD);
                put("PS Scavenge", YOUNG);
                put("ParNew", YOUNG);
            }
        };

        static GcGenerationAge fromName(String name) {
            return knownCollectors.getOrDefault(name, UNKNOWN);
        }

    }

}
