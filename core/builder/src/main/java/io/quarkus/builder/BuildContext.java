package io.quarkus.builder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.builder.location.Location;
import io.quarkus.qlue.StepContext;

/**
 * The context passed to a deployer's operation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildContext {
    private final StepContext stepContext;

    BuildContext(final StepContext stepContext) {
        this.stepContext = stepContext;
    }

    /**
     * Get the name of this build target. The resultant string is useful for diagnostic messages and does not have
     * any other significance.
     *
     * @return the name of this build target (not {@code null})
     */
    public String getBuildTargetName() {
        return stepContext.toString();
    }

    /**
     * Produce the given item. If the {@code type} refers to a item which is declared with multiplicity, then this
     * method can be called more than once for the given {@code type}, otherwise it must be called no more than once.
     *
     * @param item the item value (must not be {@code null})
     * @throws IllegalArgumentException if the item does not allow multiplicity but this method is called more than one time,
     *         or if the type of item could not be determined
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void produce(BuildItem item) {
        Assert.checkNotNullParam("item", item);
        produce((Class) item.getClass(), item);
    }

    /**
     * Produce the given items. This method can be called more than once for the given {@code type}
     *
     * @param items the items (must not be {@code null})
     * @throws IllegalArgumentException if the type of item could not be determined
     */
    public void produce(List<? extends MultiBuildItem> items) {
        Assert.checkNotNullParam("items", items);
        for (MultiBuildItem item : items) {
            produce(item);
        }
    }

    /**
     * Produce the given item. If the {@code type} refers to a item which is declared with multiplicity, then this
     * method can be called more than once for the given {@code type}, otherwise it must be called no more than once.
     *
     * @param type the item type (must not be {@code null})
     * @param item the item value (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to produce {@code type}, or if {@code type} is
     *         {@code null}, or if
     *         the item does not allow multiplicity but this method is called more than one time
     */
    public <T extends BuildItem> void produce(Class<T> type, T item) {
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepContext.produce(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class),
                    new LegacyMultiItem((MultiBuildItem) item));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepContext.produce(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class),
                    new LegacySimpleItem((SimpleBuildItem) item));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot produce an empty build item");
        }
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
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot consume single value from multi item");
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            LegacySimpleItem item = stepContext.consume(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
            return item == null ? null : type.cast(item.getItem());
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot consume an empty build item");
        }
    }

    /**
     * Consume all of the values produced for the named item. If the
     * item type implements {@link Comparable}, it will be sorted by natural order before return. The returned list
     * is a mutable copy.
     *
     * @param type the item element type (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is
     *         {@code null}
     */
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type) {
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            List<LegacyMultiItem> values = stepContext.consumeMulti(LegacyMultiItem.class,
                    type.asSubclass(MultiBuildItem.class));
            List<T> result = new ArrayList<>(values.size());
            for (int i = 0; i < values.size(); i++) {
                result.add(i, type.cast(values.get(i).getItem()));
            }
            return result;
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot consume multi value from single item");
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot consume an empty build item");
        }
    }

    /**
     * Consume all of the values produced for the named item, re-sorting it according
     * to the given comparator. The returned list is a mutable copy.
     *
     * @param type the item element type (must not be {@code null})
     * @param comparator the comparator to use (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is
     *         {@code null}
     */
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type, Comparator<? super T> comparator) {
        final List<T> result = consumeMulti(type);
        result.sort(comparator);
        return result;
    }

    /**
     * Determine if a item was produced and is therefore available to be {@linkplain #consume(Class) consumed}.
     *
     * @param type the item type (must not be {@code null})
     * @return {@code true} if the item was produced and is available, {@code false} if it was not or if this deployer does
     *         not consume the named item
     */
    public boolean isAvailableToConsume(Class<? extends BuildItem> type) {
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            return stepContext.isAvailableToConsume(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            return stepContext.isAvailableToConsume(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            return false;
        }
    }

    /**
     * Determine if a item will be consumed in this build. If a item is not consumed, then build steps are not
     * required to produce it.
     *
     * @param type the item type (must not be {@code null})
     * @return {@code true} if the item will be consumed, {@code false} if it will not be or if this deployer does
     *         not produce the named item
     */
    public boolean isConsumed(Class<? extends BuildItem> type) {
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            return stepContext.isConsumed(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            return stepContext.isConsumed(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            return stepContext.isConsumed(LegacyEmptyItem.class, type.asSubclass(EmptyBuildItem.class));
        }
    }

    /**
     * No operation.
     *
     * @param location ignored
     * @param format ignored
     * @param args ignored
     */
    @Deprecated
    public void note(Location location, String format, Object... args) {
    }

    /**
     * No operation.
     *
     * @param location ignored
     * @param format ignored
     * @param args ignored
     */
    @Deprecated
    public void warn(Location location, String format, Object... args) {
    }

    /**
     * No operation.
     *
     * @param location ignored
     * @param format ignored
     * @param args ignored
     */
    @Deprecated
    public void error(Location location, String format, Object... args) {
    }

    /**
     * Get an executor which can be used for asynchronous tasks.
     *
     * @return an executor which can be used for asynchronous tasks
     */
    public Executor getExecutor() {
        return stepContext.getExecutor();
    }
}
