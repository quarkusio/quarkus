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
import org.eclipse.microprofile.metrics.Tag;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.elementdesc.BeanInfo;
import io.smallrye.metrics.elementdesc.MemberInfo;
import io.smallrye.metrics.interceptors.MetricResolver;
import io.smallrye.metrics.setup.MetricsMetadata;

@Recorder
public class SmallRyeMetricsRecorder {

    private static final Logger log = Logger.getLogger("io.quarkus.metrics");

    // threading
    private static final String THREAD_COUNT = "thread.count";
    private static final String THREAD_DAEMON_COUNT = "thread.daemon.count";
    private static final String THREAD_MAX_COUNT = "thread.max.count";

    // class loading
    private static final String CURRENT_LOADED_CLASS_COUNT = "classloader.loadedClasses.count";
    private static final String TOTAL_LOADED_CLASS_COUNT = "classloader.loadedClasses.total";
    private static final String TOTAL_UNLOADED_CLASS_COUNT = "classloader.unloadedClasses.total";

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

    public void registerMetrics(BeanInfo beanInfo, MemberInfo memberInfo) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.APPLICATION);
        MetricsMetadata.registerMetrics(registry,
                new MetricResolver(),
                beanInfo,
                memberInfo);
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
        if (gcs.isEmpty()) {
            return;
        }
        Metadata countMetadata = Metadata.builder()
                .withName("gc.count")
                .withType(MetricType.COUNTER)
                .withDisplayName("Garbage Collection Time")
                .withUnit("none")
                .withDescription(
                        "Displays the total number of collections that have occurred. This attribute lists -1 if the collection count is undefined for this collector.")
                .build();
        Metadata timeMetadata = Metadata.builder()
                .withName("gc.time")
                .withType(MetricType.COUNTER)
                .withDisplayName("Garbage Collection Time")
                .withUnit("milliseconds")
                .withDescription(
                        "Displays the approximate accumulated collection elapsed time in milliseconds. This attribute " +
                                "displays -1 if the collection elapsed time is undefined for this collector. The Java " +
                                "virtual machine implementation may use a high resolution timer to measure the " +
                                "elapsed time. This attribute may display the same value even if the collection " +
                                "count has been incremented if the collection elapsed time is very short.")
                .build();
        for (GarbageCollectorMXBean gc : gcs) {
            registry.register(countMetadata, new LambdaCounter(() -> gc.getCollectionCount()), new Tag("name", gc.getName()));
            names.add(countMetadata.getName());

            registry.register(timeMetadata, new LambdaCounter(() -> gc.getCollectionTime()), new Tag("name", gc.getName()));
            names.add(timeMetadata.getName());
        }
    }

    private void classLoadingMetrics(MetricRegistry registry, List<String> names) {
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

        Metadata meta = Metadata.builder()
                .withName(TOTAL_LOADED_CLASS_COUNT)
                .withType(MetricType.COUNTER)
                .withDisplayName("Total Loaded Class Count")
                .withDescription(
                        "Displays the total number of classes that have been loaded since the Java virtual machine has started execution.")
                .build();
        registry.register(meta, new LambdaCounter(() -> classLoadingMXBean.getTotalLoadedClassCount()));
        names.add(TOTAL_LOADED_CLASS_COUNT);

        meta = Metadata.builder()
                .withName(TOTAL_UNLOADED_CLASS_COUNT)
                .withType(MetricType.COUNTER)
                .withDisplayName("Total Unloaded Class Count")
                .withDescription(
                        "Displays the total number of classes unloaded since the Java virtual machine has started execution.")
                .build();
        registry.register(meta, new LambdaCounter(() -> classLoadingMXBean.getUnloadedClassCount()));
        names.add(TOTAL_UNLOADED_CLASS_COUNT);

        meta = Metadata.builder()
                .withName(CURRENT_LOADED_CLASS_COUNT)
                .withType(MetricType.GAUGE)
                .withDisplayName("Current Loaded Class Count")
                .withDescription("Displays the number of classes that are currently loaded in the Java virtual machine.")
                .build();
        registry.register(meta, new LambdaGauge(() -> (long) classLoadingMXBean.getLoadedClassCount()));
        names.add(CURRENT_LOADED_CLASS_COUNT);
    }

    private void operatingSystemMetrics(MetricRegistry registry, List<String> names) {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

        Metadata meta = Metadata.builder()
                .withName(SYSTEM_LOAD_AVERAGE)
                .withType(MetricType.GAUGE)
                .withDisplayName("System Load Average")
                .withDescription("Displays the system load average for the last minute. The system load average " +
                        "is the sum of the number of runnable entities queued to the available processors and the " +
                        "number of runnable entities running on the available processors averaged over a period of time. " +
                        "The way in which the load average is calculated is operating system specific but is typically a " +
                        "damped time-dependent average. If the load average is not available, a negative value is displayed. " +
                        "This attribute is designed to provide a hint about the system load and may be queried frequently. " +
                        "The load average may be unavailable on some platforms where it is expensive to implement this method.")
                .build();
        registry.register(meta, new LambdaGauge(() -> operatingSystemMXBean.getSystemLoadAverage()));
        names.add(SYSTEM_LOAD_AVERAGE);

        meta = Metadata.builder()
                .withName(CPU_AVAILABLE_PROCESSORS)
                .withType(MetricType.GAUGE)
                .withDisplayName("Available Processors")
                .withDescription(
                        "Displays the number of processors available to the Java virtual machine. This value may change during "
                                +
                                "a particular invocation of the virtual machine.")
                .build();
        registry.register(meta, new LambdaGauge(() -> operatingSystemMXBean.getAvailableProcessors()));
        names.add(CPU_AVAILABLE_PROCESSORS);
    }

    private void threadingMetrics(MetricRegistry registry, List<String> names) {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();

        Metadata meta = Metadata.builder()
                .withName(THREAD_COUNT)
                .withType(MetricType.COUNTER)
                .withDisplayName("Thread Count")
                .withDescription("Displays the current number of live threads including both daemon and non-daemon threads")
                .build();
        registry.register(meta, new LambdaCounter(() -> (long) thread.getThreadCount()));
        names.add(THREAD_COUNT);

        meta = Metadata.builder()
                .withName(THREAD_DAEMON_COUNT)
                .withType(MetricType.COUNTER)
                .withDisplayName("Daemon Thread Count")
                .withDescription("Displays the current number of live daemon threads.")
                .build();
        registry.register(meta, new LambdaCounter(() -> (long) thread.getDaemonThreadCount()));
        names.add(THREAD_DAEMON_COUNT);

        meta = Metadata.builder()
                .withName(THREAD_MAX_COUNT)
                .withType(MetricType.COUNTER)
                .withDisplayName("Peak Thread Count")
                .withDescription("Displays the peak live thread count since the Java virtual machine started or peak was " +
                        "reset. This includes daemon and non-daemon threads.")
                .build();
        registry.register(meta, new LambdaCounter(() -> (long) thread.getPeakThreadCount()));
        names.add(THREAD_MAX_COUNT);
    }

    private void runtimeMetrics(MetricRegistry registry, List<String> names) {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        Metadata meta = Metadata.builder()
                .withName(JVM_UPTIME)
                .withType(MetricType.GAUGE)
                .withUnit(MetricUnits.MILLISECONDS)
                .withDisplayName("JVM Uptime")
                .withDescription("Displays the time from the start of the Java virtual machine in milliseconds.")
                .build();
        registry.register(meta, new LambdaGauge(() -> runtimeMXBean.getUptime()));
        names.add(JVM_UPTIME);
    }

    private void baseMemoryMetrics(MetricRegistry registry, List<String> names) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        Metadata meta = Metadata.builder()
                .withName(MEMORY_COMMITTED_HEAP)
                .withType(MetricType.GAUGE)
                .withUnit(MetricUnits.BYTES)
                .withDisplayName("Committed Heap Memory")
                .withDescription(
                        "Displays the amount of memory in bytes that is committed for the Java virtual machine to use. " +
                                "This amount of memory is guaranteed for the Java virtual machine to use.")
                .build();
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getHeapMemoryUsage().getCommitted()));
        names.add(MEMORY_COMMITTED_HEAP);

        meta = Metadata.builder()
                .withName(MEMORY_MAX_HEAP)
                .withType(MetricType.GAUGE)
                .withUnit(MetricUnits.BYTES)
                .withDisplayName("Max Heap Memory")
                .withDescription("Displays the maximum amount of heap memory in bytes that can be used for memory management. "
                        +
                        "This attribute displays -1 if the maximum heap memory size is undefined. This amount of memory is not "
                        +
                        "guaranteed to be available for memory management if it is greater than the amount of committed memory. "
                        +
                        "The Java virtual machine may fail to allocate memory even if the amount of used memory does " +
                        "not exceed this maximum size.")
                .build();
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getHeapMemoryUsage().getMax()));
        names.add(MEMORY_MAX_HEAP);

        meta = Metadata.builder()
                .withName(MEMORY_USED_HEAP)
                .withType(MetricType.GAUGE)
                .withUnit(MetricUnits.BYTES)
                .withDisplayName("Used Heap Memory")
                .withDescription("Displays the amount of used heap memory in bytes.")
                .build();
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getHeapMemoryUsage().getUsed()));
        names.add(MEMORY_USED_HEAP);
    }

    private void vendorSpecificMemoryMetrics(MetricRegistry registry, List<String> names) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        Metadata meta = Metadata.builder()
                .withName(MEMORY_COMMITTED_NON_HEAP)
                .withType(MetricType.GAUGE)
                .withUnit(MetricUnits.BYTES)
                .withDisplayName("Committed Non Heap Memory")
                .withDescription(
                        "Displays the amount of non heap memory in bytes that is committed for the Java virtual machine to use.")
                .build();
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getNonHeapMemoryUsage().getCommitted()));
        names.add(MEMORY_COMMITTED_NON_HEAP);

        meta = Metadata.builder()
                .withName(MEMORY_MAX_NON_HEAP)
                .withType(MetricType.GAUGE)
                .withUnit(MetricUnits.BYTES)
                .withDisplayName("Max Non Heap Memory")
                .withDescription("Displays the maximum amount of used non-heap memory in bytes.")
                .build();
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getNonHeapMemoryUsage().getMax()));
        names.add(MEMORY_MAX_NON_HEAP);

        meta = Metadata.builder()
                .withName(MEMORY_USED_NON_HEAP)
                .withType(MetricType.GAUGE)
                .withUnit(MetricUnits.BYTES)
                .withDisplayName("Used Non Heap Memory")
                .withDescription("Displays the amount of used non-heap memory in bytes.")
                .build();
        registry.register(meta, new LambdaGauge(() -> memoryMXBean.getNonHeapMemoryUsage().getUsed()));
        names.add(MEMORY_USED_NON_HEAP);
    }

    private void memoryPoolMetrics(MetricRegistry registry, List<String> names) {
        // MemoryPoolMXBean doesn't work in native mode
        if (!ImageInfo.inImageCode()) {
            List<MemoryPoolMXBean> mps = ManagementFactory.getMemoryPoolMXBeans();
            Metadata usageMetadata = Metadata.builder()
                    .withName("memoryPool.usage")
                    .withType(MetricType.GAUGE)
                    .withDisplayName("Current usage of the memory pool denoted by the 'name' tag")
                    .withDescription("Current usage of the memory pool denoted by the 'name' tag")
                    .withUnit(MetricUnits.BYTES)
                    .build();
            Metadata maxMetadata = Metadata.builder()
                    .withName("memoryPool.usage.max")
                    .withType(MetricType.GAUGE)
                    .withDisplayName("Peak usage of the memory pool denoted by the 'name' tag")
                    .withDescription("Peak usage of the memory pool denoted by the 'name' tag")
                    .withUnit(MetricUnits.BYTES)
                    .build();
            for (MemoryPoolMXBean mp : mps) {
                if (mp.getCollectionUsage() != null && mp.getPeakUsage() != null) {
                    // this will be the case for the heap memory pools
                    registry.register(usageMetadata, new LambdaGauge(() -> mp.getCollectionUsage().getUsed()),
                            new Tag("name", mp.getName()));
                    names.add(usageMetadata.getName());

                    registry.register(maxMetadata, new LambdaGauge(() -> mp.getPeakUsage().getUsed()),
                            new Tag("name", mp.getName()));
                    names.add(maxMetadata.getName());
                } else if (mp.getUsage() != null && mp.getPeakUsage() != null) {
                    // this will be the case for the non-heap memory pools
                    registry.register(usageMetadata, new LambdaGauge(() -> mp.getUsage().getUsed()),
                            new Tag("name", mp.getName()));
                    names.add(usageMetadata.getName());

                    registry.register(maxMetadata, new LambdaGauge(() -> mp.getPeakUsage().getUsed()),
                            new Tag("name", mp.getName()));
                    names.add(maxMetadata.getName());
                }
            }
        }
    }

}
