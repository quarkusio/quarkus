package io.quarkus.test;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

import jakarta.interceptor.InterceptorBinding;

/**
 * Activates the session context before the intercepted method is called, and terminates the context when the method invocation
 * completes (regardless of any exceptions being thrown).
 * <p>
 * If the context is already active, it's a noop - the context is neither activated nor deactivated.
 * <p>
 * Keep in mind that if the method returns an asynchronous type (such as {@link CompletionStage} then the session context is
 * still terminated when the invocation completes and not at the time the asynchronous type is completed. Also note that session
 * context is not propagated by MicroProfile Context Propagation.
 * <p>
 * This interceptor binding is only available in tests.
 */
@InterceptorBinding
@Target({ METHOD, TYPE })
@Retention(RUNTIME)
public @interface ActivateSessionContext {

}
