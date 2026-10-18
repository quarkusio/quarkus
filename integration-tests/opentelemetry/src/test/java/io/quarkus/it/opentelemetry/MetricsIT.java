package io.quarkus.it.opentelemetry;

import static io.opentelemetry.sdk.metrics.data.MetricDataType.*;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM;

import java.util.Set;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class MetricsIT extends MetricsTest {

    @Override
    protected Set<MetricToAssert> getJvmMetricsToAssert() {
        return Set.of(
                // Leaving this commented out to help with future improvements.

                // 0 on native
                //                new MetricToAssert("jvm.memory.committed", "Measure of memory committed.", "By", LONG_SUM),
                // 0 on native
                //                new MetricToAssert("jvm.memory.used", "Measure of memory used.", "By", LONG_SUM),
                // Not on native
                //new MetricToAssert("jvm.memory.limit", "Measure of max obtainable memory.", "By", LONG_SUM),
                // 0 on native
                //                new MetricToAssert("jvm.memory.used_after_last_gc",
                //                        "Measure of memory used, as measured after the most recent garbage collection event on this pool.",
                //                        "By", LONG_SUM),
                // not on native
                //                new MetricToAssert("jvm.gc.duration", "Duration of JVM garbage collection actions.", "s",
                //                        HISTOGRAM),
                // 0 on native
                //                new MetricToAssert("jvm.class.count", "Number of classes currently loaded.", "{class}",
                //                        LONG_SUM),
                // 0 on native
                //                new MetricToAssert("jvm.class.loaded", "Number of classes loaded since JVM start.", "{class}",
                //                        LONG_SUM),
                // 0 on native
                //                new MetricToAssert("jvm.class.unloaded", "Number of classes unloaded since JVM start.",
                //                        "{class}", LONG_SUM),
                new MetricToAssert("jvm.cpu.count",
                        "Number of processors available to the Java virtual machine.", "{cpu}", LONG_SUM),
                // 0 on native
                //                new MetricToAssert("jvm.cpu.limit", "", "1", LONG_SUM),
                //jvm.system.cpu.utilization instead of jvm.cpu.time
                new MetricToAssert("jvm.system.cpu.utilization", "CPU time used by the process as reported by the JVM.", "s",
                        DOUBLE_SUM),
                // 0 on native
                //                new MetricToAssert("jvm.cpu.recent_utilization",
                //                        "Recent CPU utilization for the process as reported by the JVM.", "1", DOUBLE_GAUGE),
                new MetricToAssert("jvm.cpu.longlock", "Long lock times", "s", HISTOGRAM),
                // 0 on native
                //                new MetricToAssert("jvm.cpu.context_switch", "", "Hz", DOUBLE_SUM),
                // not on native
                //                new MetricToAssert("jvm.network.io", "Network read/write bytes.", "By", HISTOGRAM), //
                //                new MetricToAssert("jvm.network.time", "Network read/write duration.", "s", HISTOGRAM), //
                new MetricToAssert("jvm.thread.count", "Number of executing platform threads.", "{thread}",
                        LONG_SUM));
    }
}
