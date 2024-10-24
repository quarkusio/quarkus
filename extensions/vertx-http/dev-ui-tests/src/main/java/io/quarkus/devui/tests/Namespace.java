package io.quarkus.devui.tests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This annotation allows to specify a namespace for an injected {@link JsonRPCServiceClient}.
 * Use this directly for test parameters to derive from the default namespace specified with {@link DevUITest}.
 *
 * <h2>Example</h2>
 *
 * <pre>
 * &#064;DevUITest( &#064;Namespace("my-namespace") )
 * class MyTestClass {
 *
 * // inject client for "my-namespace"
 * void myTestMethod(JsonRPCServiceClient client) {
 * // ...
 * }
 *
 * // inject client for "another-namespace"
 * void myTestMethod(&#064;Namespace("another-namespace") JsonRPCServiceClient client) {
 * // ...
 * }
 *
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.PARAMETER
})
@ExtendWith(DevUITestExtension.class)
public @interface Namespace {

    /**
     * The namespace (or "custom identifier") where the RPC service is registered.
     *
     * @return the namespace where the RPC service is registered
     */
    String value();

}
