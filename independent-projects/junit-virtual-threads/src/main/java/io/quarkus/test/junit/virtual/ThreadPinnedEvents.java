package io.quarkus.test.junit.virtual;

import java.util.List;

import jdk.jfr.consumer.RecordedEvent;

/**
 * Object that can be injected in a test method.
 * It gives controlled on the captured events, and so let you do manual checks.
 * <p>
 * The returned list is a copy of the list of captured events.
 */
public interface ThreadPinnedEvents extends io.quarkus.test.junit5.virtual.ThreadPinnedEvents {

    List<RecordedEvent> getEvents();

}
