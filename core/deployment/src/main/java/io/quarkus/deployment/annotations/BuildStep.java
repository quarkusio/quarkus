package io.quarkus.deployment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Indicates that a given method is a build step that is run at deployment time to
 * create the runtime output.
 * <p>
 * Build steps are run concurrently at augmentation time to augment the application. They use a producer/consumer
 * model, where a step is guaranteed not to be run until all items that it is consuming have been created.
 * <p>
 * Producing and consuming is done via injection. This can be done via field injection, method parameter injection, or
 * constructor parameter injection.
 * <p>
 * The following types are eligible for injection into a build step:
 * <ul>
 * <li>Any concrete subclass of {@link SimpleBuildItem}</li>
 * <li>{@link List} of any concrete subclass of {@link MultiBuildItem}</li>
 * <li>{@link Consumer} of any concrete subclass of {@link BuildItem}</li>
 * <li>{@link Supplier} of any concrete subclass of {@link SimpleBuildItem}</li>
 * <li>{@link Optional} instances whose value type is a subclass of {@link SimpleBuildItem}</li>
 * <li>Recorder classes, which are annotated with {@link Recorder} (method parameters only, if the method is annotated
 * {@link Record})</li>
 * <li>{@link BytecodeRecorderImpl} (method parameters only, if the method is annotated {@link Record})</li>
 * </ul>
 *
 * Injecting a {@code SimpleBuildItem} or a {@code List} of {@code MultiBuildItem} makes this step a consumer of
 * these items, and as such will not be run until all producers of the relevant items has been run.
 * <p>
 * Injecting a {@code BuildProducer} makes this class a producer of this item. Alternatively items can be produced
 * by simply returning them from the method.
 * <p>
 * If field injection is used then every {@code BuildStep} method on the class will be a producer/consumer of these
 * items, while method parameter injection is specific to an individual build step. In general method parameter injection
 * should be the preferred approach as it is more fine grained.
 * <p>
 * Note that a {@code BuildStep} will only be run if there is a consumer for items it produces. If nothing is
 * interested in the produced item then it will not be run. A consequence of this is that it must be capable of producing
 * at least one item (it does not actually have to produce anything, but it must have the ability to). A build step that
 * cannot produce anything will never be run.
 * <p>
 * {@code BuildItem} instances must be immutable, as the producer/consumer model does not allow for mutating
 * artifacts. Injecting a build item and modifying it is a bug waiting to happen, as this operation would not be accounted
 * for in the dependency graph.
 *
 * @see Record
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BuildStep {

    /**
     * Only include this build step if the given supplier class(es) return {@code true}.
     *
     * @return the supplier class array
     */
    Class<? extends BooleanSupplier>[] onlyIf() default {};

    /**
     * Only include this build step if the given supplier class(es) return {@code false}.
     *
     * @return the supplier class array
     */
    Class<? extends BooleanSupplier>[] onlyIfNot() default {};
}
