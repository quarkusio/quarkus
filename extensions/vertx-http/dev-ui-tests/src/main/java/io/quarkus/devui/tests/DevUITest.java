package io.quarkus.devui.tests;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test extension that injects DevUI-specific objects into your test. Those objects are:
 * <ul>
 * <li>{@link DevUiResourceResolver} - resolves URIs to DevUI resources</li>
 * <li>{@link JsonRPCServiceClient} - allows to communicate with a JSON RPC service during the test</li>
 * <li>{@link BuildTimeDataResolver} - allows to read build time data from extensions</li>
 * </ul>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * &#064;DevUITest(&#064;Namespace("my-namespace"))
 * class MyTestClass {
 *
 *     // Constructor Injection
 *     MyTestClass(JsonRPCServiceClient client) {
 *         // ...
 *     }
 *
 *     // Injection during test initialization
 *     &#064;BeforeEach
 *     void setup(DevUiResourceResolver resourceResolver) {
 *         // ---
 *     }
 *
 *     // Method Injection
 *     &#064;Test
 *     void myTestMethod(BuildTimeDataResolver buildTimeData) {
 *         // ...
 *     }
 *
 * }
 * </pre>
 *
 * @see org.junit.jupiter.api.extension.ParameterResolver
 * @see Namespace
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.METHOD,
        ElementType.TYPE
})
@ExtendWith(DevUITestExtension.class)
@Documented
@Inherited
public @interface DevUITest {

    /**
     * The namespace (or "custom identifier") where the RPC service is registered.
     *
     * @return the namespace where the RPC service is registered
     */
    Namespace value();

    /**
     * Specify a custom host. Otherwise, it is calculated from config value <tt>test.url</tt>.
     *
     * @return the custom host
     */
    String host() default "";

}
