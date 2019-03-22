package io.quarkus.vertx;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a business method to be automatically registered as a Vertx message consumer.
 * <p>
 * The method must accept exactly one parameter. If it accepts {@link io.vertx.core.eventbus.Message} then the return type must
 * be void. For any other
 * type the {@link io.vertx.core.eventbus.Message#body()}
 * is passed as the parameter value and the method may return an object that is passed to
 * {@link io.vertx.core.eventbus.Message#reply(Object)}, either
 * directly or via
 * {@link java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)} in case of the method returns a
 * completion stage.
 * 
 * <pre>
 * &#64;ApplicationScoped
 * class MyService {
 *
 *     &#64;ConsumeEvent("echo")
 *     String echo(String msg) {
 *         return msg.toUpperCase();
 *     }
 * 
 *     &#64;ConsumeEvent("echoMessage")
 *     void echoMessage(Message<String> msg) {
 *         msg.reply(msg.body().toUpperCase());
 *     }
 * 
 *     &#64;ConsumeEvent(value = "echoMessageBlocking", blocking = true)
 *     void echoMessageBlocking(Message<String> msg) {
 *         msg.reply(msg.body().toUpperCase());
 *     }
 * }
 * </pre>
 * 
 * @see io.vertx.core.eventbus.EventBus
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface ConsumeEvent {

    /**
     * The address the consumer will be registered to. By default, the fully qualified name of the declaring bean class is
     * assumed.
     *
     * @return the address
     */
    String value() default "";

    /**
     * 
     * @return {@code true} if the address should not be propagated across the cluster
     * @see io.vertx.core.eventbus.EventBus#localConsumer(String)
     */
    boolean local() default false;

    /**
     * 
     * @return {@code true} if the consumer should be invoked as a blocking operation using a worker thread
     * @see io.vertx.core.Vertx#executeBlocking(io.vertx.core.Handler, boolean, io.vertx.core.Handler)
     */
    boolean blocking() default false;

}
