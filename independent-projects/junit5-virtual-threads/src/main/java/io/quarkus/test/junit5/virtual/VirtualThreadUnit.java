package io.quarkus.test.junit5.virtual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit5.virtual.internal.VirtualThreadExtension;

/**
 * Extends the test case to detect pinned carrier thread.
 * <br/>
 * <br/>
 * <b>Implementation notes:</b> current implementation uses JFR under the hood, with two consequences:
 * <ol>
 * <li>This test won’t work on JVM without JFR support, e.g. OpenJ9.</li>
 * <li>Each test that uses this annotation is several seconds longer than versions without it.</li>
 * </ol>
 * This annotation uses JFR recording to detect pinning and analyze when a specific event is fired.
 * Unfortunately, to ensure no events are missed, the test must ensure the JFR recording is on and off.
 * It fires a mock event and wait until it read it.
 * Due to JFR recording API limitations, it takes a lot of time to do these loops as there are many file reads.
 * This adds several seconds to start and to stop to each test with pinned carrier thread detection enabled; and this
 * additional work is mandatory to avoid missing event.
 * <br/>
 * This behaviour is not part of API and may change in future version.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(VirtualThreadExtension.class)
public @interface VirtualThreadUnit {
}
