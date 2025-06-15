package io.quarkus.vertx;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

import io.vertx.core.eventbus.MessageCodec;
import io.vertx.mutiny.core.eventbus.Message;

/**
 * Marks a business method to be automatically registered as a Vertx message consumer.
 * <p>
 * The method can accept the following parameters:
 * <ul>
 * <li>{@link io.vertx.core.eventbus.Message io.vertx.core.eventbus.Message} message</li>
 * <li>{@link io.vertx.mutiny.core.eventbus.Message io.vertx.mutiny.core.eventbus.Message} message</li>
 * <li>{@link Message#headers() MultiMap} headers, {@link Message#body() T} body</li>
 * <li>{@link Message#body() T} body</li>
 * </ul>
 * If it accepts a {@link Message} then the return type must be void. For any other type the {@link Message#body()} is
 * passed as the parameter value and the method may return an object that is passed to {@link Message#reply(Object)},
 * either directly or via {@link java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)} in case
 * of the method returns a completion stage.
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
 *     &#64;ConsumeEvent("echoHeaders")
 *     String echo(MultiMap headers, String msg) {
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
 * If it accepts a {@link Message#headers()} then the first parameters must be the headers and the second parameter must
 * be the {@link Message#body()}.
 *
 * <pre>
 * &#64;ConsumeEvent("echoHeaders")
 * String event(MultiMap headers, String msg) {
 *     String traceId = headers.get("traceId");
 *     return "Message is: " + msg + ", trace is: " + traceId;
 * }
 * </pre>
 *
 * The CDI request context is always active during notification of the registered message consumer.
 * <p>
 * If a method annotated with {@link ConsumeEvent} throws an exception then:
 * <ul>
 * <li>if a reply handler is set the failure is propagated back to the sender via an
 * {@link io.vertx.core.eventbus.ReplyException} with code {@link #FAILURE_CODE} and the exception message,</li>
 * <li>if no reply handler is set then the exception is rethrown (and wrapped in a {@link java.lang.RuntimeException} if
 * necessary) and can be handled by the default exception handler, i.e.
 * {@link io.vertx.core.Vertx#exceptionHandler().}</li>
 * </ul>
 *
 * @see io.vertx.core.eventbus.EventBus
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface ConsumeEvent {

    /**
     * Failure code used when a message consumer method throws an exception.
     */
    int FAILURE_CODE = 0x1FF9;

    /**
     * Failure code used when a message consumer explicitly fails an asynchronous processing. This status is used when
     * the method annotated with {@link ConsumeEvent} returns a failed {@link java.util.concurrent.CompletionStage} or
     * {@link io.smallrye.mutiny.Uni}.
     */
    int EXPLICIT_FAILURE_CODE = 0x1FFF;

    /**
     * The address the consumer will be registered to. By default, the fully qualified name of the declaring bean class
     * is assumed.
     * <p>
     * The value can be a config property expression. In this case, the configured value is used instead:
     * {@code @ConsumeEvent("${my.consumer.address}")}. Additionally, the property expression can specify a default
     * value: {@code @ConsumeEvent("${my.consumer.address:defaultAddress}")}.
     *
     * @return the address
     */
    String value() default "";

    /**
     * @return {@code true} if the address should not be propagated across the cluster
     *
     * @see io.vertx.core.eventbus.EventBus#localConsumer(String)
     */
    boolean local() default true;

    /**
     * @return {@code true} if the consumer should be invoked as a blocking operation using a worker thread
     *
     * @see io.vertx.core.Vertx#executeBlocking(Callable, boolean)
     */
    boolean blocking() default false;

    /**
     * @return {@code true} if the <em>blocking</em> consumption of the event must be ordered, meaning that the method
     *         won't be called concurrently. Instead, it serializes all the invocations based on the event order.
     *         {@code ordered} must be used in conjunction with {@code blocking=true} or {@code @Blocking}.
     *
     * @see io.vertx.core.Vertx#executeBlocking(Callable, boolean)
     */
    boolean ordered() default false;

    /**
     * @return {@code null} if it should use a default MessageCodec
     *
     * @see io.quarkus.vertx.LocalEventBusCodec
     */
    Class<? extends MessageCodec> codec() default LocalEventBusCodec.class;

}
