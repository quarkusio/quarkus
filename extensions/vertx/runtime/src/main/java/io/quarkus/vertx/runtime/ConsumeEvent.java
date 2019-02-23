package io.quarkus.vertx.runtime;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

/**
 * Marks a business method to be automatically registered as a Vertx message consumer.
 * <p>
 * The method must accept exactly one parameter. If it accepts {@link Message} then the return type must be void. For any other
 * type the {@link Message#body()}
 * is passed as the parameter value and the method may return an object that is passed to {@link Message#reply(Object)}, either
 * directly or via
 * {@link CompletionStage#thenAccept(java.util.function.Consumer)} in case of the method returns a completion stage.
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
 *     &#64;ConsumeEvent("echo")
 *     void echoMessage(Message<String> msg) {
 *         msg.reply(msg.body().toUpperCase());
 *     }
 * }
 * </pre>
 * 
 * @see EventBus
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface ConsumeEvent {

    /**
     * The address a consumer will be registered to. By default, the fully qualified name of the declaring bean class is
     * assumed.
     *
     * @return the address
     */
    String value() default "";

    /**
     * 
     * @return {@code true} if the address should not be propagated across the cluster
     * @see EventBus#localConsumer(String)
     */
    boolean local() default false;

}
