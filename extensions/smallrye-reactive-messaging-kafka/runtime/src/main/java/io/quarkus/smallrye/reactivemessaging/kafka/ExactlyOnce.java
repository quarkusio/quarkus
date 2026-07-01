package io.quarkus.smallrye.reactivemessaging.kafka;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables Kafka exactly-once processing semantics on a method annotated with
 * both {@code @Incoming} and {@code @Outgoing}.
 * <p>
 * The annotated method consumes records from an incoming Kafka topic and produces results
 * to an outgoing Kafka topic within a single Kafka transaction, ensuring that consumption
 * and production are atomic — no duplicates and no message loss.
 * <p>
 * The method must use payload parameters (not {@code Message}) and both channels must be
 * managed by the Kafka connector. Both synchronous and reactive ({@code Uni}, {@code CompletionStage})
 * return types are supported.
 * <p>
 * The following Kafka properties are auto-configured:
 * <ul>
 * <li>Outgoing: {@code transactional.id}, {@code enable.idempotence=true}, {@code acks=all}</li>
 * <li>Incoming: {@code commit-strategy=ignore}, {@code isolation.level=read_committed}</li>
 * </ul>
 *
 * <pre>
 * &#64;Incoming("orders-in")
 * &#64;Outgoing("orders-out")
 * &#64;ExactlyOnce
 * Record&lt;String, Order&gt; process(Record&lt;String, Order&gt; order) {
 *     // transform and return
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExactlyOnce {
}
