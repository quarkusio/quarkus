/**
 * Copyright 2019 VMware, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.binder.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Optional;
import java.util.function.ToLongFunction;

import io.micrometer.core.lang.Nullable;

/**
 * Quarkus shade for #2330 in Micrometer 1.6.0:
 * https://github.com/micrometer-metrics/micrometer/issues/2330
 */
class JvmMemory {

    private JvmMemory() {
    }

    static Optional<MemoryPoolMXBean> getLongLivedHeapPool() {
        return ManagementFactory
                .getPlatformMXBeans(MemoryPoolMXBean.class)
                .stream()
                .filter(JvmMemory::isHeap)
                .filter(mem -> isOldGenPool(mem.getName()) || isNonGenerationalHeapPool(mem.getName()))
                .findAny();
    }

    static boolean isConcurrentPhase(String cause) {
        return "No GC".equals(cause);
    }

    static boolean isYoungGenPool(String name) {
        return name != null && name.endsWith("Eden Space");
    }

    static boolean isOldGenPool(String name) {
        return name != null && (name.endsWith("Old Gen") || name.endsWith("Tenured Gen"));
    }

    static boolean isNonGenerationalHeapPool(String name) {
        return "Shenandoah".equals(name) || "ZHeap".equals(name);
    }

    private static boolean isHeap(MemoryPoolMXBean memoryPoolBean) {
        return MemoryType.HEAP.equals(memoryPoolBean.getType());
    }

    static double getUsageValue(MemoryPoolMXBean memoryPoolMXBean, ToLongFunction<MemoryUsage> getter) {
        MemoryUsage usage = getUsage(memoryPoolMXBean);
        if (usage == null) {
            return Double.NaN;
        }
        return getter.applyAsLong(usage);
    }

    @Nullable
    private static MemoryUsage getUsage(MemoryPoolMXBean memoryPoolMXBean) {
        try {
            return memoryPoolMXBean.getUsage();
        } catch (InternalError e) {
            // Defensive for potential InternalError with some specific JVM options. Based on its Javadoc,
            // MemoryPoolMXBean.getUsage() should return null, not throwing InternalError, so it seems to be a JVM bug.
            return null;
        }
    }
}
