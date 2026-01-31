package io.quarkus.test.junit.virtual.internal;

import static io.quarkus.test.junit.virtual.internal.Collector.CARRIER_PINNED_EVENT_NAME;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Enhanced test event that simulates the jdk.VirtualThreadPinned event
 * with the additional fields added in Java 24/25.
 *
 * Real fields in Java 24+:
 * - pinnedReason (String)
 * - blockingOperation (String)
 * - carrierThreadId (Thread) - though we can't easily set this in a custom event
 */
@Label("Test Custom Event with Java 25 Fields")
@Category("Application Events")
@Description("This event simulates thread pinning with Java 24/25 fields")
@Name(CARRIER_PINNED_EVENT_NAME)
public class TestPinJfrEventJava25 extends Event {

    @Label("Pinned Reason")
    @Description("The reason why the virtual thread was pinned")
    private final String pinnedReason;

    @Label("Blocking Operation")
    @Description("The blocking operation that occurred while pinned")
    private final String blockingOperation;

    public TestPinJfrEventJava25(String pinnedReason, String blockingOperation) {
        this.pinnedReason = pinnedReason;
        this.blockingOperation = blockingOperation;
    }

    public static void pin() {
        TestPinJfrEventJava25 event = new TestPinJfrEventJava25(
                "Native method or synchronized monitor",
                "Object.wait");
        event.commit();
    }

    public static void pinWithDetails(String reason, String operation) {
        TestPinJfrEventJava25 event = new TestPinJfrEventJava25(reason, operation);
        event.commit();
    }
}
