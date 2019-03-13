/*
 * Copyright 2018 Red Hat, Inc.
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

package io.quarkus.smallrye.metrics.runtime;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;
import io.smallrye.metrics.MetricRegistries;

@Template
public class SmallRyeMetricsTemplate {

    private static final Logger log = Logger.getLogger("io.quarkus.metrics");

    // threading
    private static final String THREAD_COUNT = "thread.count";
    private static final String THREAD_DAEMON_COUNT = "thread.daemon.count";
    private static final String THREAD_MAX_COUNT = "thread.max.count";

    // class loading
    private static final String CURRENT_LOADED_CLASS_COUNT = "classloader.currentLoadedClass.count";
    private static final String TOTAL_LOADED_CLASS_COUNT = "classloader.totalLoadedClass.count";
    private static final String TOTAL_UNLOADED_CLASS_COUNT = "classloader.totalUnloadedClass.count";

    // runtime
    private static final String JVM_UPTIME = "jvm.uptime";

    // operating system
    private static final String SYSTEM_LOAD_AVERAGE = "cpu.systemLoadAverage";
    private static final String CPU_AVAILABLE_PROCESSORS = "cpu.availableProcessors";

    // memory
    private static final String MEMORY_COMMITTED_NON_HEAP = "memory.committedNonHeap";
    private static final String MEMORY_COMMITTED_HEAP = "memory.committedHeap";
    private static final String MEMORY_MAX_HEAP = "memory.maxHeap";
    private static final String MEMORY_MAX_NON_HEAP = "memory.maxNonHeap";
    private static final String MEMORY_USED_HEAP = "memory.usedHeap";
    private static final String MEMORY_USED_NON_HEAP = "memory.usedNonHeap";

    public void registerVendorMetrics(ShutdownContext shutdown) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        List<String> names = new ArrayList<>();

        memoryPoolMetrics(registry, names);
        vendorSpecificMemoryMetrics(registry, names);

        if (!names.isEmpty()) {
            shutdown.addShutdownTask(() -> {
                for (String i : names) {
                    registry.remove(i);
                }
            });
        }
    }

    public void registerBaseMetrics(ShutdownContext shutdown) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        List<String> names = new ArrayList<>();

        garbageCollectionMetrics(registry, names);
        classLoadingMetrics(registry, names);
        operatingSystemMetrics(registry, names);
        threadingMetrics(registry, names);
        runtimeMetrics(registry, names);
        baseMemoryMetrics(registry, names);

        if (!names.isEmpty()) {
            shutdown.addShutdownTask(() -> {
                for (String i : names) {
                    registry.remove(i);
                }
            });
        }
    }

    public void createRegistries(BeanContainer container) {
        MetricRegistries.get(MetricRegistry.Type.APPLICATION);
        MetricRegistries.get(MetricRegistry.Type.BASE);
        MetricRegistries.get(MetricRegistry.Type.VENDOR);

        //HACK: registration is done via statics, but cleanup is done via pre destroy
        //however if the bean is not used it will not be created, so no cleanup will be done
        //we force bean creation here to make sure the container can restart correctly
        container.instance(MetricRegistries.class).getApplicationRegistry();
    }

    private void garbageCollectionMetrics(MetricRegistry registry, List<String> names) {
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            Metadata meta = new Metadata("gc." + gc.getName() + ".count", MetricType.COUNTER);
            meta.setDisplayName("Garbage Collection Time");
            meta.setUnit("none");
            meta.setDescription(
                    "Displays the total number of collections that have occurred. This attribute lists -1 if the collection count is undefined for this collector.");
            registry.register(meta, new LambdaCounter(() -> gc.getCollectionCount()));
            names.add(meta.getName());

            meta = new Metadata("gc." + gc.getName() + ".time", MetricType.COUNTER);
            meta.setDisplayName("Garbage Collection Time");
            meta.setUnit("milliseconds");
            meta.setDescription(
                    "Displays the approximate accumulated collection elapsed time in milliseconds. This attribute " +
                            "displays -1 if the collection elapsed time is undefined for this collector. The Java virtual " +
                            "machine implementation may use a high resolution timer to measure the elapsed time. This " +
                            "attribute may display the same value even if the collection count has been incremented if " +
                            "the collection elapsed time is very short.");
            registry.register(meta, new LambdaCounter(() -> gc.getCollectionTime()));
            names.add(meta.getName());
        }
    }

    private void classLoadingMetrics(MetricRegistry registry, List<String> names) {
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

        Metadata meta = new Metadata(TOTAL_LOADED_CLASS_COUNT, MetricType.COUNTER);
        meta.setDisplayName("Total Loaded Class Count");
        meta.setDescription(
                "Displays the total number of classes that have been loaded since the Java virtual machine has started execution.");
        registry.register(meta, new LambdaCounter(() -> classLoadingMXBean.getTotalLoadedClassCount()));
        names.add(TOTAL_LOADED_CLASS_COUNT);

        meta = new Metadata(TOTAL_UNLOADED_CLASS_COUNT, MetricType.COUNTER);
        meta.setDisplayName("Total Unloaded Class Count");
        meta.setDescription(
                "Displays the total number of classes unloaded since the Java virtual machine has started execution.");
        registry.register(meta, new LambdaCounter(() -> classLoadingMXBean.getUnloadedClassCount()));
        names.add(TOTAL_UNLOADED_CLASS_COUNT);

        meta = new Metadata(CURRENT_LOADED_CLASS_COUNT, MetricType.COUNTER);
        meta.setDisplayName("Current Loaded Class Count");
        meta.setDescription("Displays the number of classes that are currently loaded in the Java virtual machine.");
        registry.register(meta, new LambdaCounter(() -> (long) classLoadingMXBean.getLoadedClassCount()));
        names.add(CURRENT_LOADED_CLASS_COUNT);
    }

    private void operatingSystemMetrics(MetricRegistry registry, List<String> names) {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

        Metadata meta = new Metadata(SYSTEM_LOAD_AVERAGE, MetricType.GAUGE);
        meta.setDisplayName("System Load Average");
        meta.setDescription("Displays the system load average for the last minute. The system load average " +
                "is the sum of the number of runnable entities queued to the available processors and the " +
                "number of runnable entities running on the available processors averaged over a period of time. " +
                "The way in which the load average is calculated is operating system specific but is typically a " +
                "damped time-dependent average. If the load average is not available, a negative value is displayed. " +
                "This attribute is designed to provide a hint about the system load and may be queried frequently. " +
                "The load average may be unavailable on some platforms where it is expensive to implement this method.");
        registry.register(meta, new LambdaGauge(() -> operatingSystemMXBean.getSystemLoadAverage()));
        names.add(SYSTEM_LOAD_AVERAGE);

        meta = new Metadata(CPU_AVAILABLE_PROCESSORS, MetricType.GAUGE);
        meta.setDisplayName("Available Processors");
        meta.setDescription(
                "Displays the number of processors available to the Java virtual machine. This value may change during " +
                        "a particular invocation of the virtual machine.");
        registry.register(meta, new LambdaGauge(() -> operatingSystemMXBean.getAvailableProcessors()));
        names.add(CPU_AVAILABLE_PROCESSORS);
    }

    private void threadingMetrics(MetricRegistry registry, List<String> names) {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();

        Metadata meta = new Metadata(THREAD_COUNT, MetricType.COUNTER);
        meta.setDisplayName("Thread Count");
        meta.setDescription("Displays the current number of live threads including both daemon and non-daemon threads");
        registry.register(meta, new LambdaCounter(() -> (long) thread.getThreadCount()));
        names.add(THREAD_COUNT);

        meta = new Metadata(THREAD_DAEMON_COUNT, MetricType.COUNTER);
        meta.setDisplayName("Daemon Thread Count");
        meta.setDescription("Displays the current number of live daemon threads.");
        registry.register(meta, new LambdaCounter(() -> (long) thread.getDaemonThreadCount()));
        names.add(THREAD_DAEMON_COUNT);

        meta = new Metadata(THREAD_MAX_COUNT, MetricType.COUNTER);
        meta.setDisplayName("Peak Thread Count");
        meta.setDescription("Displays the peak live thread count since the Java virtual machine started or peak was " +
                "reset. This includes daemon and non-daemon threads.");
        registry.register(meta, new LambdaCounter(() -> (long) thread.getPeakThreadCount()));
        names.add(THREAD_MAX_COUNT);
    }

    private void runtimeMetrics(MetricRegistry registry, List<String> names) {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        Metadata meta = new Metadata(JVM_UPTIME, MetricType.GAUGE, MetricUnits.MILLISECONDS);
        meta.setDisplayName("JVM Uptime");
        meta.setDescription("Displays the time from the start of the Java virtual machine in milliseconds.");
        registry.register(meta, new LambdaGauge(() -> runtimeMXBean.getUptime()));
        names.add(JVM_UPTIME);
    }

    private void baseMemoryMetrics(MetricRegistry registry, List<String> names) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        Metadata meta = new Metadata(MEMORY_COMMITTED_HEAP, MetricType.GAUGE, MetricUnits.BYTES);
        meta.setDisplayName("Committed Heap Memory");
        meta.setDescription("Displays the amount of memory in bytes that is committed for the Java virtual machine to use. " +
                "This amount of memory is guaranteed for the Java virtual machine to use.");
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getHeapMemoryUsage().getCommitted()));
        names.add(MEMORY_COMMITTED_HEAP);

        meta = new Metadata(MEMORY_MAX_HEAP, MetricType.GAUGE, MetricUnits.BYTES);
        meta.setDisplayName("Max Heap Memory");
        meta.setDescription("Displays the maximum amount of heap memory in bytes that can be used for memory management. " +
                "This attribute displays -1 if the maximum heap memory size is undefined. This amount of memory is not " +
                "guaranteed to be available for memory management if it is greater than the amount of committed memory. " +
                "The Java virtual machine may fail to allocate memory even if the amount of used memory does " +
                "not exceed this maximum size.");
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getHeapMemoryUsage().getMax()));
        names.add(MEMORY_MAX_HEAP);

        meta = new Metadata(MEMORY_USED_HEAP, MetricType.GAUGE, MetricUnits.BYTES);
        meta.setDisplayName("Used Heap Memory");
        meta.setDescription("Displays the amount of used heap memory in bytes.");
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getHeapMemoryUsage().getUsed()));
        names.add(MEMORY_USED_HEAP);
    }

    private void vendorSpecificMemoryMetrics(MetricRegistry registry, List<String> names) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        Metadata meta = new Metadata(MEMORY_COMMITTED_NON_HEAP, MetricType.GAUGE, MetricUnits.BYTES);
        meta.setDisplayName("Committed Non Heap Memory");
        meta.setDescription(
                "Displays the amount of non heap memory in bytes that is committed for the Java virtual machine to use.");
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getNonHeapMemoryUsage().getCommitted()));
        names.add(MEMORY_COMMITTED_NON_HEAP);

        meta = new Metadata(MEMORY_MAX_NON_HEAP, MetricType.GAUGE, MetricUnits.BYTES);
        meta.setDisplayName("Max Non Heap Memory");
        meta.setDescription("Displays the maximum amount of used non-heap memory in bytes.");
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getNonHeapMemoryUsage().getMax()));
        names.add(MEMORY_MAX_NON_HEAP);

        meta = new Metadata(MEMORY_USED_NON_HEAP, MetricType.GAUGE, MetricUnits.BYTES);
        meta.setDisplayName("Used Non Heap Memory");
        meta.setDescription("Displays the amount of used non-heap memory in bytes.");
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getNonHeapMemoryUsage().getUsed()));
        names.add(MEMORY_USED_NON_HEAP);
    }

    private void memoryPoolMetrics(MetricRegistry registry, List<String> names) {
        // MemoryPoolMXBean doesn't work in native mode
        if (!ImageInfo.inImageCode()) {
            List<MemoryPoolMXBean> mps = ManagementFactory.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean mp : mps) {
                if (mp.getCollectionUsage() != null && mp.getPeakUsage() != null) {
                    Metadata usageMetadata = new Metadata("memoryPool." + mp.getName() + ".usage", MetricType.GAUGE);
                    usageMetadata.setDisplayName("Current usage of the " + mp.getName() + " memory pool");
                    usageMetadata.setUnit("bytes");
                    usageMetadata.setDescription("Current usage of the " + mp.getName() + " memory pool");
                    registry.register(usageMetadata, new LambdaGauge(() -> mp.getCollectionUsage().getUsed()));
                    names.add(usageMetadata.getName());

                    Metadata maxMetadata = new Metadata("memoryPool." + mp.getName() + ".usage.max", MetricType.GAUGE);
                    maxMetadata.setDisplayName("Peak usage of the " + mp.getName() + " memory pool");
                    maxMetadata.setUnit("bytes");
                    maxMetadata.setDescription("Peak usage of the " + mp.getName() + " memory pool");
                    registry.register(maxMetadata, new LambdaGauge(() -> mp.getPeakUsage().getUsed()));
                    names.add(maxMetadata.getName());
                }
            }
        }
    }

}
