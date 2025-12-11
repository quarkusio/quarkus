package io.quarkus.test.junit.virtual.internal;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Internal events used during the capture.
 */
public interface InternalEvents {

    String INITIALIZATION_EVENT_NAME = "internal.io.quarkus.test.junit.virtual.InternalEvents.InitializationEvent";
    String SHUTDOWN_EVENT_NAME = "internal.io.quarkus.test.junit.virtual.InternalEvents.ShutdownEvent";

    String CAPTURING_STARTED_EVENT_NAME = "internal.io.quarkus.test.junit.virtual.InternalEvents.CapturingStartedEvent";
    String CAPTURING_STOPPED_EVENT_NAME = "internal.io.quarkus.test.junit.virtual.InternalEvents.CapturingStoppedEvent";

    @Name(INITIALIZATION_EVENT_NAME)
    @Category("virtual-thread-unit")
    @StackTrace(value = false)
    class InitializationEvent extends Event {
        // Marker event
    }

    @Name(SHUTDOWN_EVENT_NAME)
    @Category("virtual-thread-unit")
    @StackTrace(value = false)
    class ShutdownEvent extends Event {
        // Marker event
    }

    @Name(CAPTURING_STARTED_EVENT_NAME)
    @Category("virtual-thread-unit")
    @StackTrace(value = false)
    class CapturingStartedEvent extends Event {

        @Name("id")
        @Label("id")
        public final String id;

        public CapturingStartedEvent(String id) {
            this.id = id;
        }
    }

    @Name(CAPTURING_STOPPED_EVENT_NAME)
    @Category("virtual-thread-unit")
    @StackTrace(value = false)
    class CapturingStoppedEvent extends Event {

        @Name("id")
        @Label("id")
        public final String id;

        public CapturingStoppedEvent(String id) {
            this.id = id;
        }
    }
}
