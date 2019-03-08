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

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;
import io.smallrye.metrics.MetricRegistries;

@Template
public class SmallRyeMetricsTemplate {

    private static final Logger log = Logger.getLogger("io.quarkus.metrics");
    private static final String MEMORY_HEAP_USAGE = "memory.heap.usage";
    private static final String MEMORY_NON_HEAP_USAGE = "memory.nonHeap.usage";
    private static final String THREAD_COUNT = "thread.count";

    public void registerBaseMetrics(ShutdownContext shutdown) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        List<String> names = new ArrayList<>();
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
                    "machine implementation may use a high resolution timer to measure the elapsed time. This attribute may display the same value even if the collection count has been incremented if the collection elapsed time is very short.");
            registry.register(meta, new LambdaCounter(() -> gc.getCollectionTime()));
            names.add(meta.getName());

        }
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                for (String i : names) {
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
        registry.register(meta, new LambdaCounter(() -> (long) thread.getThreadCount()));

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                registry.remove(MEMORY_HEAP_USAGE);
                registry.remove(MEMORY_NON_HEAP_USAGE);
                registry.remove(THREAD_COUNT);

            }
        });

        /*
         * meta = new Metadata("thread.cpuTime", MetricType.COUNTER);
         * meta.setUnit("milliseconds");
         * registry.register(meta, new LambdaCounter( ()->thread.getCurrentThreadCpuTime()));
         */
        /*
         * List<MemoryPoolMXBean> mps = ManagementFactory.getMemoryPoolMXBeans();
         * for (MemoryPoolMXBean mp : mps) {
         * Metadata meta = new Metadata("memoryPool." + mp.getName() + ".usage", MetricType.GAUGE);
         * meta.setDisplayName( "Current usage of the " + mp.getName() + " memory pool");
         * meta.setUnit("bytes");
         * meta.setDescription( "Current usage of the " + mp.getName() + " memory pool");
         * registry.register( meta, new LambdaGauge( ()-> mp.getCollectionUsage().getUsed() ));
         * 
         * meta = new Metadata("memoryPool." + mp.getName() + ".usage.max", MetricType.GAUGE);
         * meta.setDisplayName( "Peak usage of the " + mp.getName() + " memory pool");
         * meta.setUnit("bytes");
         * meta.setDescription( "Peak usage of the " + mp.getName() + " memory pool");
         * registry.register( meta, new LambdaGauge( ()-> mp.getPeakUsage().getUsed()));
         * }
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
