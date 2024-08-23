package io.quarkus.test.junit5.virtual.internal;

import static io.quarkus.test.junit5.virtual.internal.Collector.CARRIER_PINNED_EVENT_NAME;

import jdk.jfr.*;

@Label("Test Custom Event")
@Category("Application Events")
@Description("This event is for testing thread pinning")
@Name(CARRIER_PINNED_EVENT_NAME)
public class TestPinJfrEvent extends Event {

    @Label("Pin Message")
    private final String message;

    public TestPinJfrEvent(String message) {
        this.message = message;
    }

    public static void pin() {
        TestPinJfrEvent event = new TestPinJfrEvent("Hello, JFR!");
        event.commit();
    }
}
