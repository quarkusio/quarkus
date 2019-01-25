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

package org.jboss.shamrock.metrics.runtime;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.jboss.logging.Logger;
import org.jboss.shamrock.arc.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ShutdownContext;
import org.jboss.shamrock.runtime.Template;

import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.app.CounterImpl;

/**
 * Created by bob on 7/30/18.
 */
@Template
public class MetricsDeploymentTemplate {

    private static final Logger log = Logger.getLogger("org.jboss.shamrock.metrics");
    private static final String MEMORY_HEAP_USAGE = "memory.heap.usage";
    private static final String MEMORY_NON_HEAP_USAGE = "memory.nonHeap.usage";
    private static final String THREAD_COUNT = "thread.count";

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
    public void registerCounted(String topClassName, String elementName, ShutdownContext shutdown) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.APPLICATION);

        String name = MetricRegistry.name(topClassName, elementName);
        Metadata meta = new Metadata(name, MetricType.COUNTER);
        log.debugf("Register: %s", name);
        registry.register(meta, new CounterImpl());
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                registry.remove(name);
            }
        });
    }

    public void registerBaseMetrics(ShutdownContext shutdown) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        List<String> names = new ArrayList<>();
        for (GarbageCollectorMXBean gc : gcs) {
            Metadata meta = new Metadata("gc." + gc.getName() + ".count", MetricType.COUNTER);
            meta.setDisplayName("Garbage Collection Time");
            meta.setUnit("none");
            meta.setDescription("Displays the total number of collections that have occurred. This attribute lists -1 if the collection count is undefined for this collector.");
            registry.register(meta, new LambdaCounter(() -> gc.getCollectionCount()));
            names.add(meta.getName());

            meta = new Metadata("gc." + gc.getName() + ".time", MetricType.COUNTER);
            meta.setDisplayName("Garbage Collection Time");
            meta.setUnit("milliseconds");
            meta.setDescription("machine implementation may use a high resolution timer to measure the elapsed time. This attribute may display the same value even if the collection count has been incremented if the collection elapsed time is very short.");
            registry.register(meta, new LambdaCounter(() -> gc.getCollectionTime()));
            names.add(meta.getName());

        }
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                for(String i : names) {
                    registry.remove(i);
                }
            }
        });
    }

    public void registerVendorMetrics(ShutdownContext shutdown) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

        Metadata meta = new Metadata(MEMORY_HEAP_USAGE, MetricType.GAUGE);
        meta.setUnit("bytes");
        registry.register(meta, new LambdaGauge(() -> mem.getHeapMemoryUsage().getUsed()));

        meta = new Metadata(MEMORY_NON_HEAP_USAGE, MetricType.GAUGE);
        meta.setUnit("bytes");
        registry.register(meta, new LambdaGauge(() -> mem.getNonHeapMemoryUsage().getUsed()));

        ThreadMXBean thread = ManagementFactory.getThreadMXBean();

        meta = new Metadata(THREAD_COUNT, MetricType.COUNTER);
        registry.register(meta, new LambdaCounter( ()-> (long) thread.getThreadCount() ) );

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                registry.remove(MEMORY_HEAP_USAGE);
                registry.remove(MEMORY_NON_HEAP_USAGE);
                registry.remove(THREAD_COUNT);

            }
        });

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

    public void createRegistries(BeanContainer container) {
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
