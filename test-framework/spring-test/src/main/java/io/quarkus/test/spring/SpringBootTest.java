package io.quarkus.test.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTestExtension;

/**
 * Quarkus equivalent of Spring Boot's {@code @SpringBootTest}.
 * <p>
 * Annotate your test class with this annotation to start a Quarkus application
 * for the test. The only change required when migrating from Spring Boot tests
 * is updating the import:
 *
 * <pre>
 *
 * // Before (Spring Boot):
 * import org.springframework.boot.test.context.SpringBootTest;
 *
 * // After (Quarkus):
 * import io.quarkus.test.spring.SpringBootTest;
 * </pre>
 */
@ExtendWith(QuarkusTestExtension.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpringBootTest {
}
