package io.quarkus.test.component;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.quarkus.test.component.TestConfigProperty.TestConfigProperties;

/**
 * Set the value of a configuration property.
 * <p>
 * If declared on a class then the configuration property is used for all test methods declared on the test class.
 * <p>
 * If declared on a method then the configuration property is only used for that test method.
 * If the test instance lifecycle is {@link Lifecycle#_PER_CLASS}, this annotation can only be used on the test class and is
 * ignored on test methods.
 * <p>
 * Configuration properties declared on test methods take precedence over the configuration properties declared on test class.
 *
 * @see QuarkusComponentTest
 * @see QuarkusComponentTestExtensionBuilder#configProperty(String, String)
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Repeatable(TestConfigProperties.class)
public @interface TestConfigProperty {

    String key();

    String value();

    @Retention(RUNTIME)
    @Target({ TYPE, METHOD })
    @interface TestConfigProperties {

        TestConfigProperty[] value();

    }

}
