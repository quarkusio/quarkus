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

import static org.jboss.builder.Execution.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.builder.diag.Diagnostic;
import org.jboss.builder.item.NamedBuildItem;
import org.jboss.builder.item.NamedMultiBuildItem;
import org.jboss.builder.location.Location;
import org.jboss.builder.item.MultiBuildItem;
import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.SimpleBuildItem;
import org.wildfly.common.Assert;

/**
 * The context passed to a deployer's operation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildContext {
    private final StepInfo stepInfo;
    private final Execution execution;
    private final AtomicInteger dependencies;

    BuildContext(final StepInfo stepInfo, final Execution execution) {
        this.stepInfo = stepInfo;
        this.execution = execution;
        dependencies = new AtomicInteger(stepInfo.getDependencies());
    }

    /**
     * Get the name of this build target.  The resultant string is useful for diagnostic messages and does not have
     * any other significance.
     *
     * @return the name of this build target (not {@code null})
     */
    public String getBuildTargetName() {
        return execution.getBuildTargetName();
    }

    /**
     * Produce the given item.  If the {@code type} refers to a item which is declared with multiplicity, then this
     * method can be called more than once for the given {@code type}, otherwise it must be called no more than once.
     *
     * @param item the item value (must not be {@code null})
     * @throws IllegalArgumentException if the item does not allow multiplicity but this method is called more than one time,
     * or if the type of item could not be determined
     */
    public void produce(BuildItem item) {
        Assert.checkNotNullParam("item", item);
        if (item instanceof NamedBuildItem) {
            throw new IllegalArgumentException("Cannot produce a named build item without a name");
        }
        doProduce(new ItemId(item.getClass(), null), item);
    }

    /**
     * Produce the given items.  This method can be called more than once for the given {@code type}
     *
     * @param items the items (must not be {@code null})
     * @throws IllegalArgumentException if the type of item could not be determined
     */
    public void produce(List<? extends MultiBuildItem> items) {
        Assert.checkNotNullParam("items", items);
        for(MultiBuildItem item : items) {
            doProduce(new ItemId(item.getClass(), null), item);
        }
    }

    /**
     * Produce the given item.  If the {@code type} refers to a item which is declared with multiplicity, then this
     * method can be called more than once for the given {@code type}, otherwise it must be called no more than once.
     *
     * @param type the item type (must not be {@code null})
     * @param item the item value (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to produce {@code type}, or if {@code type} is {@code null}, or if
     * the item does not allow multiplicity but this method is called more than one time
     */
    public <T extends BuildItem> void produce(Class<T> type, T item) {
        Assert.checkNotNullParam("type", type);
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot produce a named build item without a name");
        }
        doProduce(new ItemId(type, null), type.cast(item));
    }

    /**
     * Produce the given item.  If the {@code type} refers to a item which is declared with multiplicity, then this
     * method can be called more than once for the given {@code type}, otherwise it must be called no more than once.
     *
     * @param name the build item name (must not be {@code null})
     * @param item the item value (must not be {@code null})
     * @throws IllegalArgumentException if the item does not allow multiplicity but this method is called more than one time,
     * or if the type of item could not be determined
     */
    public <N> void produce(N name, NamedBuildItem<N> item) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("item", item);
        doProduce(new ItemId(item.getClass(), name), item);
    }

    /**
     * Produce the given item.  If the {@code type} refers to a item which is declared with multiplicity, then this
     * method can be called more than once for the given {@code type}, otherwise it must be called no more than once.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @param item the item value (must not be {@code null})
     * @throws IllegalArgumentException if the item does not allow multiplicity but this method is called more than one time,
     * or if the type of item could not be determined
     */
    public <N, T extends NamedBuildItem<N>> void produce(Class<T> type, N name, NamedBuildItem<N> item) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("item", item);
        doProduce(new ItemId(type, name), item);
    }

    /**
     * Consume the value produced for the named item.
     *
     * @param type the item type (must not be {@code null})
     * @return the produced item (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is {@code null}
     * @throws ClassCastException if the cast failed
     */
    public <T extends SimpleBuildItem> T consume(Class<T> type) {
        Assert.checkNotNullParam("type", type);
        final ItemId id = new ItemId(type, null);
        if (id.isMulti()) {
            throw Messages.msg.cannotMulti(id);
        }
        if (! stepInfo.getConsumes().contains(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        return type.cast(execution.getSingles().get(id));
    }

    /**
     * Consume the value produced for the named item.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @return the produced item (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is {@code null}
     * @throws ClassCastException if the cast failed
     */
    public <N, T extends NamedBuildItem<N>> T consume(Class<T> type, N name) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        final ItemId id = new ItemId(type, name);
        if (id.isMulti()) {
            throw Messages.msg.cannotMulti(id);
        }
        if (! stepInfo.getConsumes().contains(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        return type.cast(execution.getSingles().get(id));
    }

    /**
     * Consume all of the values produced for the named item.  If the
     * item type implements {@link Comparable}, it will be sorted by natural order before return.  The returned list
     * is a mutable copy.
     *
     * @param type the item element type (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is {@code null}
     */
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type) {
        Assert.checkNotNullParam("type", type);
        final ItemId id = new ItemId(type, null);
        if (! id.isMulti()) {
            // can happen if obj changes base class
            throw Messages.msg.cannotMulti(id);
        }
        if (! stepInfo.getConsumes().contains(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        return new ArrayList<>((List<T>) (List) execution.getMultis().getOrDefault(id, Collections.emptyList()));
    }

    /**
     * Consume all of the values produced for the named item.  If the
     * item type implements {@link Comparable}, it will be sorted by natural order before return.  The returned list
     * is a mutable copy.
     *
     * @param type the item element type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is {@code null}
     */
    public <N, T extends NamedMultiBuildItem<N>> List<T> consumeMulti(Class<T> type, N name) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        final ItemId id = new ItemId(type, name);
        if (! id.isMulti()) {
            // can happen if obj changes base class
            throw Messages.msg.cannotMulti(id);
        }
        if (! stepInfo.getConsumes().contains(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        return new ArrayList<>((List<T>) (List) execution.getMultis().getOrDefault(id, Collections.emptyList()));
    }

    /**
     * Consume all of the values produced for the named item, re-sorting it according
     * to the given comparator.  The returned list is a mutable copy.
     *
     * @param type the item element type (must not be {@code null})
     * @param comparator the comparator to use (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is {@code null}
     */
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type, Comparator<? super T> comparator) {
        final List<T> result = consumeMulti(type);
        result.sort(comparator);
        return result;
    }

    /**
     * Consume all of the values produced for the named item, re-sorting it according
     * to the given comparator.  The returned list is a mutable copy.
     *
     * @param type the item element type (must not be {@code null})
     * @param comparator the comparator to use (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is {@code null}
     */
    public <N, T extends NamedMultiBuildItem<N>> List<T> consumeMulti(Class<T> type, N name, Comparator<? super T> comparator) {
        final List<T> result = consumeMulti(type, name);
        result.sort(comparator);
        return result;
    }

    /**
     * Determine if a item was produced and is therefore available to be {@linkplain #consume(Class) consumed}.
     *
     * @param type the item type (must not be {@code null})
     * @return {@code true} if the item was produced and is available, {@code false} if it was not or if this deployer does
     * not consume the named item
     */
    public boolean isAvailableToConsume(Class<? extends BuildItem> type) {
        final ItemId id = new ItemId(type, null);
        return stepInfo.getConsumes().contains(id) && id.isMulti() ? ! execution.getMultis().getOrDefault(id, Collections.emptyList()).isEmpty() : execution.getSingles().containsKey(id);
    }

    /**
     * Determine if a item was produced and is therefore available to be {@linkplain #consume(Class) consumed}.
     *
     * @param type the item type (must not be {@code null})
     * @return {@code true} if the item was produced and is available, {@code false} if it was not or if this deployer does
     * not consume the named item
     */
    public <N> boolean isAvailableToConsume(Class<? extends NamedBuildItem<N>> type, N name) {
        final ItemId id = new ItemId(type, name);
        return stepInfo.getConsumes().contains(id) && id.isMulti() ? ! execution.getMultis().getOrDefault(id, Collections.emptyList()).isEmpty() : execution.getSingles().containsKey(id);
    }

    /**
     * Determine if a item will be consumed in this build.  If a item is not consumed, then build steps are not
     * required to produce it.
     *
     * @param type the item type (must not be {@code null})
     * @return {@code true} if the item will be consumed, {@code false} if it will not be or if this deployer does
     * not produce the named item
     */
    public boolean isConsumed(Class<? extends BuildItem> type) {
        return execution.getBuildChain().getConsumed().contains(new ItemId(type, null));
    }


    /**
     * Determine if a item will be consumed in this build.  If a item is not consumed, then build steps are not
     * required to produce it.
     *
     * @param type the item type (must not be {@code null})
     * @return {@code true} if the item will be consumed, {@code false} if it will not be or if this deployer does
     * not produce the named item
     */
    public <N> boolean isConsumed(Class<? extends NamedBuildItem<N>> type, N name) {
        return execution.getBuildChain().getConsumed().contains(new ItemId(type, name));
    }

    /**
     * Emit a build note.  This indicates information that the user may be interested in.
     *
     * @param location the location of interest (may be {@code null})
     * @param format the format string (see {@link String#format(String, Object...)})
     * @param args the format arguments
     */
    public void note(Location location, String format, Object... args) {
        final List<Diagnostic> list = execution.getDiagnostics();
        synchronized (list) {
            list.add(new Diagnostic(Diagnostic.Level.NOTE, location, format, args));
        }
    }

    /**
     * Emit a build warning.  This indicates a significant build problem that the user should be made aware of.
     *
     * @param location the location of interest (may be {@code null})
     * @param format the format string (see {@link String#format(String, Object...)})
     * @param args the format arguments
     */
    public void warn(Location location, String format, Object... args) {
        final List<Diagnostic> list = execution.getDiagnostics();
        synchronized (list) {
            list.add(new Diagnostic(Diagnostic.Level.WARN, location, format, args));
        }
    }

    /**
     * Emit a build error.  This indicates a build problem that prevents the build from proceeding.
     *
     * @param location the location of interest (may be {@code null})
     * @param format the format string (see {@link String#format(String, Object...)})
     * @param args the format arguments
     */
    public void error(Location location, String format, Object... args) {
        final List<Diagnostic> list = execution.getDiagnostics();
        synchronized (list) {
            list.add(new Diagnostic(Diagnostic.Level.ERROR, location, format, args));
        }
        execution.setErrorReported();
    }

    /**
     * Get an executor which can be used for asynchronous tasks.
     *
     * @return an executor which can be used for asynchronous tasks
     */
    public Executor getExecutor() {
        return execution.getExecutor();
    }

    // -- //

    private void doProduce(ItemId id, BuildItem value) {
        if (! stepInfo.getProduces().contains(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        if (id.isMulti()) {
            final List<BuildItem> list = execution.getMultis().computeIfAbsent(id, x -> new ArrayList<>());
            synchronized (list) {
                if (Comparable.class.isAssignableFrom(id.getType())) {
                    int pos = Collections.binarySearch((List) list, value);
                    if (pos < 0) pos = -(pos + 1);
                    list.add(pos, value);
                } else {
                    list.add(value);
                }
            }
        } else {
            if (execution.getSingles().putIfAbsent(id, value) != null) {
                throw Messages.msg.cannotMulti(id);
            }
        }
    }

    void depFinished() {
        final int remaining = dependencies.decrementAndGet();
        log.tracef("Dependency of \"%2$s\" finished; %1$d remaining", remaining, stepInfo.getBuildStep());
        if (remaining == 0) {
            execution.getExecutor().execute(this::run);
        }
    }

    void run() {
        final Execution execution = this.execution;
        final StepInfo stepInfo = this.stepInfo;
        final BuildStep buildStep = stepInfo.getBuildStep();
        log.tracef("Starting step \"%s\"", buildStep);
        try {
            if (! execution.isErrorReported()) {
                try {
                    buildStep.execute(this);
                } catch (Throwable t) {
                    final List<Diagnostic> list = execution.getDiagnostics();
                    synchronized (list) {
                        list.add(new Diagnostic(Diagnostic.Level.ERROR, t, null, "Build step %s threw an exception", buildStep));
                    }
                    execution.setErrorReported();
                }
            }
        } finally {
            log.tracef("Finished step \"%s\"", buildStep);
            execution.removeBuildContext(stepInfo, this);
        }
        final Set<StepInfo> dependents = stepInfo.getDependents();
        if (! dependents.isEmpty()) {
            for (StepInfo info : dependents) {
                execution.getBuildContext(info).depFinished();
            }
        } else {
            execution.depFinished();
        }
    }
}
