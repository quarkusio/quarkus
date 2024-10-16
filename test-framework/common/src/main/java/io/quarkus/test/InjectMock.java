package io.quarkus.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Instructs the test engine to inject a mock instance of a bean into the annotated field or parameter.
 * <p>
 * This annotation is supported:
 * <ul>
 * <li>for fields and method parameters in a {@code io.quarkus.test.component.QuarkusComponentTest},</li>
 * <li>for fields in a {@code io.quarkus.test.QuarkusTest} if {@code quarkus-junit5-mockito} is present.</li>
 * </ul>
 * The lifecycle and configuration API of the injected mock depends on the type of test being used.
 * <p>
 * Some test types impose additional restrictions and limitations. For example, only beans that have a
 * <a href="https://quarkus.io/guides/cdi#client_proxies">client proxy</a> may be mocked in a
 * {@code io.quarkus.test.junit.QuarkusTest}.
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
public @interface InjectMock {

}
