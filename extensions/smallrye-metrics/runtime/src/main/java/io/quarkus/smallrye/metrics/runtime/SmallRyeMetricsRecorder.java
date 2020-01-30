package io.quarkus.smallrye.metrics.runtime;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.inject.spi.BeanManager;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.metrics.ExtendedMetadata;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.TagsUtils;
import io.smallrye.metrics.elementdesc.BeanInfo;
import io.smallrye.metrics.elementdesc.MemberInfo;
import io.smallrye.metrics.interceptors.MetricResolver;
import io.smallrye.metrics.setup.MetricsMetadata;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

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
    private static final String SYSTEM_CPU_LOAD = "cpu.systemCpuLoad";
    private static final String PROCESS_CPU_LOAD = "cpu.processCpuLoad";
    private static final String PROCESS_CPU_TIME = "cpu.processCpuTime";
    private static final String FREE_PHYSICAL_MEM_SIZE = "memory.freePhysicalSize";
    private static final String FREE_SWAP_SIZE = "memory.freeSwapSize";

    // memory
    private static final String MEMORY_COMMITTED_NON_HEAP = "memory.committedNonHeap";
    private static final String MEMORY_COMMITTED_HEAP = "memory.committedHeap";
    private static final String MEMORY_MAX_HEAP = "memory.maxHeap";
    private static final String MEMORY_MAX_NON_HEAP = "memory.maxNonHeap";
    private static final String MEMORY_USED_HEAP = "memory.usedHeap";
    private static final String MEMORY_USED_NON_HEAP = "memory.usedNonHeap";

    public Function<Router, Route> route(String name) {
        return new Function<Router, Route>() {
            @Override
            public Route apply(Router router) {
                return router.route(name);
            }
        };
    }

    public SmallRyeMetricsHandler handler(String metricsPath) {
        SmallRyeMetricsHandler handler = new SmallRyeMetricsHandler();
        handler.setMetricsPath(metricsPath);
        return handler;
    }

    public void registerVendorMetrics(ShutdownContext shutdown) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        List<String> names = new ArrayList<>();

        memoryPoolMetrics(registry, names);
        vendorSpecificMemoryMetrics(registry, names);
        vendorOperatingSystemMetrics(registry, names);

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
        baseOperatingSystemMetrics(registry, names);
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

    public void registerMicrometerJvmMetrics(ShutdownContext shutdown) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        List<String> names = new ArrayList<>();

        micrometerJvmGcMetrics(registry, names, shutdown);
        micrometerJvmThreadMetrics(registry, names);
        micrometerJvmMemoryMetrics(registry, names);
        micrometerJvmClassLoaderMetrics(registry, names);

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

    public void registerMetricFromProducer(String beanId, MetricType metricType,
            String metricName, String[] tags, String description,
            String displayName, String unit) {
        ArcContainer container = Arc.container();
        InjectableBean<Object> injectableBean = container.bean(beanId);
        BeanManager beanManager = container.beanManager();
        Metric reference = (Metric) beanManager.getReference(injectableBean, Metric.class,
                beanManager.createCreationalContext(injectableBean));
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.APPLICATION);
        Metadata metadata = Metadata.builder()
                .withType(metricType)
                .withName(metricName)
                .withDescription(description)
                .withDisplayName(displayName)
                .withUnit(unit)
                .notReusable()
                .build();
        registry.register(metadata, reference, TagsUtils.parseTagsAsArray(tags));
    }

    public void registerMetric(MetricRegistry.Type scope,
            MetadataHolder metadataHolder,
            TagHolder[] tagHolders,
            Object implementor,
            ShutdownContext shutdown) {
        Metadata metadata = metadataHolder.toMetadata();
        Tag[] tags = Arrays.stream(tagHolders).map(TagHolder::toTag).toArray(Tag[]::new);
        MetricRegistry registry = MetricRegistries.get(scope);

        switch (metadata.getTypeRaw()) {
            case GAUGE:
                registry.register(metadata, (Gauge) implementor, tags);
                break;
            case TIMER:
                if (implementor == null) {
                    registry.timer(metadata, tags);
                } else {
                    registry.register(metadata, (Timer) implementor);
                }
                break;
            case COUNTER:
                if (implementor == null) {
                    registry.counter(metadata, tags);
                } else {
                    registry.register(metadata, (Counter) implementor, tags);
                }
                break;
            case HISTOGRAM:
                if (implementor == null) {
                    registry.histogram(metadata, tags);
                } else {
                    registry.register(metadata, (Histogram) implementor, tags);
                }
                break;
            case CONCURRENT_GAUGE:
                if (implementor == null) {
                    registry.concurrentGauge(metadata, tags);
                } else {
                    registry.register(metadata, (ConcurrentGauge) implementor, tags);
                }
                break;
            case METERED:
                if (implementor == null) {
                    registry.meter(metadata, tags);
                } else {
                    registry.register(metadata, (Metered) implementor, tags);
                }
                break;
        }
        shutdown.addShutdownTask(() -> registry.remove(metadata.getName()));
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
                .withName("gc.total")
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

    private void baseOperatingSystemMetrics(MetricRegistry registry, List<String> names) {
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

        // some metrics are only available in jdk internal class 'com.sun.management.OperatingSystemMXBean': cast to it.
        // com.sun.management.OperatingSystemMXBean is not available in SubstratVM
        // the cast will fail for some JVM not derived from HotSpot (J9 for example) so we check if it is assignable to it
        if (!ImageInfo.inImageCode()
                && com.sun.management.OperatingSystemMXBean.class.isAssignableFrom(operatingSystemMXBean.getClass())) {
            try {
                com.sun.management.OperatingSystemMXBean internalOperatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
                meta = Metadata.builder()
                        .withName(PROCESS_CPU_LOAD)
                        .withType(MetricType.GAUGE)
                        .withUnit(MetricUnits.PERCENT)
                        .withDisplayName("Process CPU load")
                        .withDescription("Displays  the \"recent cpu usage\" for the Java Virtual Machine process. " +
                                "This value is a double in the [0.0,1.0] interval. A value of 0.0 means that none of " +
                                "the CPUs were running threads from the JVM process during the recent period of time " +
                                "observed, while a value of 1.0 means that all CPUs were actively running threads from " +
                                "the JVM 100% of the time during the recent period being observed. Threads from the JVM " +
                                "include the application threads as well as the JVM internal threads. " +
                                "All values betweens 0.0 and 1.0 are possible depending of the activities going on in " +
                                "the JVM process and the whole system. " +
                                "If the Java Virtual Machine recent CPU usage is not available, the method returns a negative value.")
                        .build();
                registry.register(meta, new LambdaGauge(() -> internalOperatingSystemMXBean.getProcessCpuLoad()));
                names.add(PROCESS_CPU_LOAD);
            } catch (ClassCastException cce) {
                // this should never occurs
                log.debug("Unable to cast the OperatingSystemMXBean to com.sun.management.OperatingSystemMXBean, " +
                        "not registering extended operating system metrics", cce);
            }
        }
    }

    private void vendorOperatingSystemMetrics(MetricRegistry registry, List<String> names) {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

        // some metrics are only available in jdk internal class 'com.sun.management.OperatingSystemMXBean': cast to it.
        // com.sun.management.OperatingSystemMXBean is not available in SubstratVM
        // the cast will fail for some JVM not derived from HotSpot (J9 for example) so we check if it is assignable to it
        if (!ImageInfo.inImageCode()
                && com.sun.management.OperatingSystemMXBean.class.isAssignableFrom(operatingSystemMXBean.getClass())) {
            try {
                com.sun.management.OperatingSystemMXBean internalOperatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
                Metadata meta = Metadata.builder()
                        .withName(SYSTEM_CPU_LOAD)
                        .withType(MetricType.GAUGE)
                        .withUnit(MetricUnits.PERCENT)
                        .withDisplayName("System CPU load")
                        .withDescription("Displays the \"recent cpu usage\" for the whole system. This value is a double " +
                                "in the [0.0,1.0] interval. A value of 0.0 means that all CPUs were idle during the recent " +
                                "period of time observed, while a value of 1.0 means that all CPUs were actively running " +
                                "100% of the time during the recent period being observed. " +
                                "All values betweens 0.0 and 1.0 are possible depending of the activities going on in the " +
                                "system. If the system recent cpu usage is not available, the method returns a negative value.")
                        .build();
                registry.register(meta, new LambdaGauge(() -> internalOperatingSystemMXBean.getSystemCpuLoad()));
                names.add(SYSTEM_CPU_LOAD);

                meta = Metadata.builder()
                        .withName(PROCESS_CPU_TIME)
                        .withType(MetricType.GAUGE)
                        .withUnit(MetricUnits.NANOSECONDS)
                        .withDisplayName("Process CPU time")
                        .withDescription(
                                "Displays the CPU time used by the process on which the Java virtual machine is running " +
                                        "in nanoseconds. The returned value is of nanoseconds precision but not necessarily " +
                                        "nanoseconds accuracy. This method returns -1 if the the platform does not support " +
                                        "this operation.")
                        .build();
                registry.register(meta, new LambdaGauge(() -> internalOperatingSystemMXBean.getProcessCpuTime()));
                names.add(PROCESS_CPU_TIME);

                meta = Metadata.builder()
                        .withName(FREE_PHYSICAL_MEM_SIZE)
                        .withType(MetricType.GAUGE)
                        .withUnit(MetricUnits.BYTES)
                        .withDisplayName("Free physical memory size")
                        .withDescription("Displays the amount of free physical memory in bytes.")
                        .build();
                registry.register(meta, new LambdaGauge(() -> internalOperatingSystemMXBean.getFreePhysicalMemorySize()));
                names.add(FREE_PHYSICAL_MEM_SIZE);

                meta = Metadata.builder()
                        .withName(FREE_SWAP_SIZE)
                        .withType(MetricType.GAUGE)
                        .withUnit(MetricUnits.BYTES)
                        .withDisplayName("Free swap size")
                        .withDescription("Displays the amount of free swap space in bytes.")
                        .build();
                registry.register(meta, new LambdaGauge(() -> internalOperatingSystemMXBean.getFreePhysicalMemorySize()));
                names.add(FREE_SWAP_SIZE);
            } catch (ClassCastException cce) {
                // this should never occurs
                log.debug("Unable to cast the OperatingSystemMXBean to com.sun.management.OperatingSystemMXBean, " +
                        "not registering extended operating system metrics", cce);
            }
        }
    }

    private void threadingMetrics(MetricRegistry registry, List<String> names) {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();

        Metadata meta = Metadata.builder()
                .withName(THREAD_COUNT)
                .withType(MetricType.GAUGE)
                .withDisplayName("Thread Count")
                .withDescription("Displays the current number of live threads including both daemon and non-daemon threads")
                .build();
        registry.register(meta, new LambdaGauge(() -> (long) thread.getThreadCount()));
        names.add(THREAD_COUNT);

        meta = Metadata.builder()
                .withName(THREAD_DAEMON_COUNT)
                .withType(MetricType.GAUGE)
                .withDisplayName("Daemon Thread Count")
                .withDescription("Displays the current number of live daemon threads.")
                .build();
        registry.register(meta, new LambdaGauge(() -> (long) thread.getDaemonThreadCount()));
        names.add(THREAD_DAEMON_COUNT);

        meta = Metadata.builder()
                .withName(THREAD_MAX_COUNT)
                .withType(MetricType.GAUGE)
                .withDisplayName("Peak Thread Count")
                .withDescription("Displays the peak live thread count since the Java virtual machine started or peak was " +
                        "reset. This includes daemon and non-daemon threads.")
                .build();
        registry.register(meta, new LambdaGauge(() -> (long) thread.getPeakThreadCount()));
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

    private void micrometerJvmGcMetrics(MetricRegistry registry, List<String> names, ShutdownContext shutdown) {
        if (!ImageInfo.inImageCode()) {
            MicrometerGCMetrics gcMetrics = new MicrometerGCMetrics();

            registry.register(new ExtendedMetadata("jvm.gc.max.data.size",
                    MetricType.GAUGE,
                    MetricUnits.BYTES,
                    "Max size of old generation memory pool",
                    true), new LambdaGauge(gcMetrics::getMaxDataSize));
            names.add("jvm.gc.max.data.size");
            registry.register(new ExtendedMetadata("jvm.gc.live.data.size",
                    MetricType.GAUGE,
                    MetricUnits.BYTES,
                    "Size of old generation memory pool after a full GC",
                    true), new LambdaGauge(gcMetrics::getLiveDataSize));
            names.add("jvm.gc.live.data.size");
            registry.register(new ExtendedMetadata("jvm.gc.memory.promoted",
                    MetricType.COUNTER,
                    MetricUnits.BYTES,
                    "Count of positive increases in the size of the old generation memory pool before GC to after GC",
                    true,
                    "jvm_gc_memory_promoted_bytes_total"), new LambdaCounter(gcMetrics::getPromotedBytes));
            names.add("jvm.gc.memory.promoted");
            registry.register(new ExtendedMetadata("jvm.gc.memory.allocated",
                    MetricType.COUNTER,
                    MetricUnits.BYTES,
                    "Incremented for an increase in the size of the young generation memory pool after one GC to before the next",
                    true,
                    "jvm_gc_memory_allocated_bytes_total"), new LambdaCounter(gcMetrics::getAllocatedBytes));
            names.add("jvm.gc.memory.allocated");

            // start updating the metric values in a listener for GC events
            // Metrics that mimic the jvm.gc.pause timer will be registered lazily as GC events occur
            gcMetrics.startWatchingNotifications();
            shutdown.addShutdownTask(gcMetrics::cleanUp);
        }
    }

    private void micrometerJvmThreadMetrics(MetricRegistry registry, List<String> names) {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        registry.register(
                new ExtendedMetadata("jvm.threads.peak",
                        MetricType.GAUGE,
                        "threads",
                        "The peak live thread count since the Java virtual machine started or peak was reset",
                        true),
                new LambdaGauge(threadBean::getPeakThreadCount));
        names.add("jvm.threads.peak");
        registry.register(
                new ExtendedMetadata("jvm.threads.daemon",
                        MetricType.GAUGE,
                        "threads",
                        "The current number of live daemon threads",
                        true),
                new LambdaGauge(threadBean::getDaemonThreadCount));
        names.add("jvm.threads.daemon");
        registry.register(
                new ExtendedMetadata("jvm.threads.live",
                        MetricType.GAUGE,
                        "threads",
                        "The current number of live threads including both daemon and non-daemon threads",
                        true),
                new LambdaGauge(threadBean::getThreadCount));
        names.add("jvm.threads.live");

        if (!ImageInfo.inImageCode()) {
            ExtendedMetadata threadStatesMetadata = new ExtendedMetadata("jvm.threads.states",
                    MetricType.GAUGE,
                    "threads",
                    "The current number of threads having a particular state",
                    true);
            for (Thread.State state : Thread.State.values()) {
                registry.register(threadStatesMetadata,
                        new LambdaGauge(() -> getThreadStateCount(threadBean, state)),
                        new Tag("state", state.name().toLowerCase().replace("_", "-")));
                names.add("jvm.threads.states");
            }
        }
    }

    private void micrometerJvmMemoryMetrics(MetricRegistry registry, List<String> names) {
        if (!ImageInfo.inImageCode()) {
            for (MemoryPoolMXBean memoryPoolMXBean : ManagementFactory.getMemoryPoolMXBeans()) {
                String area = MemoryType.HEAP.equals(memoryPoolMXBean.getType()) ? "heap" : "nonheap";
                Tag[] tags = new Tag[] { new Tag("id", memoryPoolMXBean.getName()),
                        new Tag("area", area) };

                registry.register(
                        new ExtendedMetadata("jvm.memory.used",
                                MetricType.GAUGE,
                                "bytes",
                                "The amount of used memory",
                                true),
                        new LambdaGauge(() -> memoryPoolMXBean.getUsage().getUsed()),
                        tags);
                names.add("jvm.memory.used");

                registry.register(
                        new ExtendedMetadata("jvm.memory.committed",
                                MetricType.GAUGE,
                                "bytes",
                                "The amount of memory in bytes that is committed for the Java virtual machine to use",
                                true),
                        new LambdaGauge(() -> memoryPoolMXBean.getUsage().getCommitted()),
                        tags);
                names.add("jvm.memory.committed");

                registry.register(
                        new ExtendedMetadata("jvm.memory.max",
                                MetricType.GAUGE,
                                "bytes",
                                "The maximum amount of memory in bytes that can be used for memory management",
                                true),
                        new LambdaGauge(() -> memoryPoolMXBean.getUsage().getMax()),
                        tags);
                names.add("jvm.memory.max");
            }

            for (BufferPoolMXBean bufferPoolBean : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
                Tag tag = new Tag("id", bufferPoolBean.getName());

                registry.register(
                        new ExtendedMetadata("jvm.buffer.count",
                                MetricType.GAUGE,
                                "buffers",
                                "An estimate of the number of buffers in the pool",
                                true),
                        new LambdaGauge(() -> bufferPoolBean.getCount()),
                        tag);
                names.add("jvm.buffer.count");

                registry.register(
                        new ExtendedMetadata("jvm.buffer.memory.used",
                                MetricType.GAUGE,
                                "bytes",
                                "An estimate of the memory that the Java virtual machine is using for this buffer pool",
                                true),
                        new LambdaGauge(() -> bufferPoolBean.getMemoryUsed()),
                        tag);
                names.add("jvm.buffer.memory.used");

                registry.register(
                        new ExtendedMetadata("jvm.buffer.total.capacity",
                                MetricType.GAUGE,
                                "bytes",
                                "An estimate of the total capacity of the buffers in this pool",
                                true),
                        new LambdaGauge(() -> bufferPoolBean.getTotalCapacity()),
                        tag);
                names.add("jvm.buffer.total.capacity");
            }

        }
    }

    private void micrometerJvmClassLoaderMetrics(MetricRegistry registry, List<String> names) {
        // The ClassLoadingMXBean can be used in native mode, but it only returns zeroes, so there's no point in including such metrics.
        if (!ImageInfo.inImageCode()) {
            ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();

            registry.register(
                    new ExtendedMetadata("jvm.classes.loaded",
                            MetricType.GAUGE,
                            "classes",
                            "The number of classes that are currently loaded in the Java virtual machine",
                            true,
                            "jvm_classes_loaded_classes"),
                    new LambdaGauge(() -> classLoadingBean.getLoadedClassCount()));
            names.add("jvm.classes.loaded");

            registry.register(
                    new ExtendedMetadata("jvm.classes.unloaded",
                            MetricType.COUNTER,
                            "classes",
                            "The total number of classes unloaded since the Java virtual machine has started execution",
                            true,
                            "jvm_classes_unloaded_classes_total"),
                    new LambdaCounter(() -> classLoadingBean.getUnloadedClassCount()));
            names.add("jvm.classes.unloaded");
        }
    }

    private long getThreadStateCount(ThreadMXBean threadBean, Thread.State state) {
        return Arrays.stream(threadBean.getThreadInfo(threadBean.getAllThreadIds()))
                .filter(threadInfo -> threadInfo != null && threadInfo.getThreadState() == state)
                .count();
    }

}
