package io.quarkus.test.junit5.virtual;

import java.util.List;

import jdk.jfr.consumer.RecordedEvent;

/**
 * Object that can be injected in a test method.
 * It gives controlled on the captured events, and so let you do manual checks.
 * <p>
 * The returned list is a copy of the list of captured events.
 *
 * @deprecated use {@link io.quarkus.test.junit.virtual.ThreadPinnedEvents} instead
 */
@Deprecated(since = "3.31", forRemoval = true)
public interface ThreadPinnedEvents {

    List<RecordedEvent> getEvents();

}
