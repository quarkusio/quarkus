package org.jboss.shamrock.metrics.runtime;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ContextObject;

import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.app.CounterImpl;
import io.smallrye.metrics.interceptors.MetricResolver;

/**
 * Created by bob on 7/30/18.
 */
public class MetricsDeploymentTemplate {

    private static final Logger log = Logger.getLogger("org.jboss.shamrock.metrics");

    /*
            public <E extends Member & AnnotatedElement> void registerCounted(Class<?> topClass, E element) {
                MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.APPLICATION);
                //registry.register(name, new CounterImpl());
                MetricResolver resolver = new MetricResolver();
                MetricResolver.Of<Counted> of = resolver.counted(topClass, element);
                Metadata meta = new Metadata(of.metricName(), MetricType.COUNTER);
                //registry.register(meta, new CounterImpl());
            }
            */
    public void registerCounted(String topClassName, String elementName) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.APPLICATION);

        String name = MetricRegistry.name(topClassName, elementName);
        Metadata meta = new Metadata(name, MetricType.COUNTER);
        log.debugf("Register: %s", name);
        registry.register(meta, new CounterImpl());
    }

    public void registerBaseMetrics() {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            Metadata meta = new Metadata("gc." + gc.getName() + ".count", MetricType.COUNTER);
            meta.setDisplayName("Garbage Collection Time");
            meta.setUnit("none");
            meta.setDescription("Displays the total number of collections that have occurred. This attribute lists -1 if the collection count is undefined for this collector.");
            registry.register(meta, new LambdaCounter(() -> gc.getCollectionCount()));

            meta = new Metadata("gc." + gc.getName() + ".time", MetricType.COUNTER);
            meta.setDisplayName("Garbage Collection Time");
            meta.setUnit("milliseconds");
            meta.setDescription("machine implementation may use a high resolution timer to measure the elapsed time. This attribute may display the same value even if the collection count has been incremented if the collection elapsed time is very short.");
            registry.register(meta, new LambdaCounter(() -> gc.getCollectionTime()));
        }
    }

    public void registerVendorMetrics() {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

        Metadata meta = new Metadata("memory.heap.usage", MetricType.GAUGE);
        meta.setUnit("bytes");
        registry.register(meta, new LambdaGauge(() -> mem.getHeapMemoryUsage().getUsed()));

        meta = new Metadata("memory.nonHeap.usage", MetricType.GAUGE);
        meta.setUnit("bytes");
        registry.register(meta, new LambdaGauge(() -> mem.getNonHeapMemoryUsage().getUsed()));

        ThreadMXBean thread = ManagementFactory.getThreadMXBean();

        meta = new Metadata("thread.count", MetricType.COUNTER);
        registry.register(meta, new LambdaCounter( ()-> (long) thread.getThreadCount() ) );

        /*
        meta = new Metadata("thread.cpuTime", MetricType.COUNTER);
        meta.setUnit("milliseconds");
        registry.register(meta, new LambdaCounter( ()->thread.getCurrentThreadCpuTime()));
        */
            /*
        List<MemoryPoolMXBean> mps = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean mp : mps) {
            Metadata meta = new Metadata("memoryPool." + mp.getName() + ".usage", MetricType.GAUGE);
            meta.setDisplayName( "Current usage of the " + mp.getName() + " memory pool");
            meta.setUnit("bytes");
            meta.setDescription( "Current usage of the " + mp.getName() + " memory pool");
            registry.register( meta, new LambdaGauge( ()-> mp.getCollectionUsage().getUsed() ));

            meta = new Metadata("memoryPool." + mp.getName() + ".usage.max", MetricType.GAUGE);
            meta.setDisplayName( "Peak usage of the " + mp.getName() + " memory pool");
            meta.setUnit("bytes");
            meta.setDescription( "Peak usage of the " + mp.getName() + " memory pool");
            registry.register( meta, new LambdaGauge( ()-> mp.getPeakUsage().getUsed()));
        }
            */


    }

    public void createRegistries(@ContextObject("bean.container") BeanContainer container) {
        log.info("Creating registries");
        MetricRegistries.get(MetricRegistry.Type.APPLICATION);
        MetricRegistries.get(MetricRegistry.Type.BASE);
        MetricRegistries.get(MetricRegistry.Type.VENDOR);

        //HACK: registration is does via statics, but cleanup is done via pre destroy
        //however if the bean is not used it will not be created, so no cleanup will be done
        //we force bean creation here to make sure the container can restart correctly
        container.instance(MetricRegistries.class).getApplicationRegistry();
    }
}
