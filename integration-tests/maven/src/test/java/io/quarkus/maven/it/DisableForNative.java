package io.quarkus.maven.it;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@DisabledIfSystemProperty(named = "quarkus.test.native", matches = "true")
public @interface DisableForNative {
}
