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

package org.jboss.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jboss.builder.diag.Diagnostic;
import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.MultiBuildItem;
import org.jboss.builder.item.SimpleBuildItem;

/**
 * The final result of a successful deployment operation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildResult {
    private final ConcurrentHashMap<ItemId, BuildItem> simpleItems;
    private final ConcurrentHashMap<ItemId, List<BuildItem>> multiItems;
    private final List<Diagnostic> diagnostics;
    private final long nanos;

    BuildResult(final ConcurrentHashMap<ItemId, BuildItem> simpleItems,
            final ConcurrentHashMap<ItemId, List<BuildItem>> multiItems, final Set<ItemId> finalIds,
            final List<Diagnostic> diagnostics, final long nanos) {
        this.simpleItems = simpleItems;
        this.multiItems = multiItems;
        this.diagnostics = diagnostics;
        this.nanos = nanos;
    }

    /**
     * Consume the value produced for the named item.
     *
     * @param type the item type (must not be {@code null})
     * @return the produced item (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is
     *         {@code null}
     * @throws ClassCastException if the cast failed
     */
    public <T extends SimpleBuildItem> T consume(Class<T> type) {
        final ItemId itemId = new ItemId(type, null);
        final Object item = simpleItems.get(itemId);
        if (item == null) {
            throw Messages.msg.undeclaredItem(itemId);
        }
        return type.cast(item);
    }

    /**
     * Consume all of the values produced for the named item.
     *
     * @param type the item element type (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}
     */
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type) {
        final ItemId itemId = new ItemId(type, null);
        @SuppressWarnings("unchecked")
        final List<T> items = (List<T>) (List) multiItems.get(itemId);
        if (items == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(items);
    }

    /**
     * Get the diagnostics reported during build.
     *
     * @return the diagnostics reported during build
     */
    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    /**
     * Get the amount of elapsed time from the time the operation was initiated to the time it was completed.
     *
     * @param timeUnit the time unit to return
     * @return the time
     */
    public long getDuration(TimeUnit timeUnit) {
        return timeUnit.convert(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Close all the resultant resources, logging any failures.
     */
    public void closeAll() throws RuntimeException {
        for (BuildItem obj : simpleItems.values()) {
            if (obj instanceof AutoCloseable)
                try {
                    ((AutoCloseable) obj).close();
                } catch (Exception e) {
                    Messages.msg.closeFailed(obj, e);
                }
        }
        for (List<? extends BuildItem> list : multiItems.values()) {
            for (BuildItem obj : list) {
                if (obj instanceof AutoCloseable)
                    try {
                        ((AutoCloseable) obj).close();
                    } catch (Exception e) {
                        Messages.msg.closeFailed(obj, e);
                    }
            }
        }
    }
}
