package io.quarkus.vertx.runtime;

import io.quarkus.arc.runtime.BeanInvoker;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.Message;

/**
 * Invokes a business method annotated with {@link ConsumeEvent}.
 */
public interface EventConsumerInvoker extends BeanInvoker<Message<Object>> {

}
