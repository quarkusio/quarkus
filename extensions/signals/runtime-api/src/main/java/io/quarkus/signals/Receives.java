package io.quarkus.signals;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identifies the signal parameter of a receiver method declared on a CDI bean.
 *
 * <p>
 * A receiver method is a non-private non-static method that has exactly one parameter annotated with {@code @Receives}. The
 * type of the annotated parameter determines the received signal type. Qualifiers declared on the annotated parameter are used
 * during type-safe resolution. If a receiver declares no qualifier, it has exactly one qualifier &mdash; the default qualifier
 * {@code @Default}.
 *
 * <pre>
 * void onOrder(&#064;Receives OrderPlaced order) {
 *     // has qualifiers [@Default]; only receives signals emitted with no qualifier
 * }
 *
 * void onUrgentOrder(&#064;Receives &#064;Urgent OrderPlaced order) {
 *     // has qualifiers [@Urgent]; only receives signals emitted with the &#064;Urgent qualifier
 * }
 *
 * void onAnyOrder(&#064;Receives &#064;Any OrderPlaced order) {
 *     // has qualifiers [@Any]; receives all OrderPlaced signals
 * }
 * </pre>
 *
 * The fact that a receiver which declares no qualifier has exactly one qualifier &mdash; {@code @Default} &mdash; is the only
 * difference from the CDI specification, where similar observer methods have an empty set of qualifiers and therefore match any
 * events of a specific type.
 * <p>
 * Additional parameters may be declared and are treated as CDI injection points &mdash; the container will obtain and inject
 * the corresponding bean instances when the receiver is invoked.
 *
 * <p>
 * The annotated parameter may also be of type {@link SignalContext}, in which case the receiver has access to
 * the full signal context including metadata, qualifiers, and emission type:
 *
 * <pre>
 * void onOrder(&#064;Receives SignalContext&lt;OrderPlaced&gt; ctx) {
 *     OrderPlaced order = ctx.signal();
 *     Map&lt;String, Object&gt; meta = ctx.metadata();
 * }
 * </pre>
 *
 * <p>
 * Receiver methods are inherited by bean subclasses.
 *
 * @see Signal
 */
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Receives {

}
