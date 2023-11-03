package io.quarkus.test.junit5.virtual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit5.virtual.internal.VirtualThreadExtension;

/**
 * Extends the test case to detect pinned carrier thread.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(VirtualThreadExtension.class)
public @interface VirtualThreadUnit {
}
