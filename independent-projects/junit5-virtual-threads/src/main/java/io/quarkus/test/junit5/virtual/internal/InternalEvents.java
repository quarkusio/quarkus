package io.quarkus.test.junit5.virtual.internal;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Internal events used during the capture.
 */
public interface InternalEvents {

    String INITIALIZATION_EVENT_NAME = "io.quarkus.test.junit5.virtual.internal.InternalEvents.InitializationEvent";
    String SHUTDOWN_EVENT_NAME = "io.quarkus.test.junit5.virtual.internal.InternalEvents.ShutdownEvent";

    String CAPTURING_STARTED_EVENT_NAME = "io.quarkus.test.junit5.virtual.internal.InternalEvents.CapturingStartedEvent";
    String CAPTURING_STOPPED_EVENT_NAME = "io.quarkus.test.junit5.virtual.internal.InternalEvents.CapturingStoppedEvent";

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
