package io.quarkus.core.impl;

import java.util.concurrent.ConcurrentHashMap;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * A JFR-enabled service monitor that fires JDK Flight Recorder events
 * for service lifecycle transitions.
 */
public final class JfrServiceMonitor extends ServiceMonitor {

    /**
     * Map holding active startup events indexed by service name.
     */
    private final ConcurrentHashMap<String, ServiceStartEvent> activeStarts = new ConcurrentHashMap<>();

    /**
     * Map holding active stop events indexed by service name.
     */
    private final ConcurrentHashMap<String, ServiceStopEvent> activeStops = new ConcurrentHashMap<>();

    /**
     * The singleton instance of the JFR service monitor.
     */
    public static final JfrServiceMonitor INSTANCE = new JfrServiceMonitor();

    /**
     * Private constructor.
     */
    private JfrServiceMonitor() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startInitiated(String name) {
        ServiceStartEvent event = new ServiceStartEvent();
        event.serviceName = name;
        event.begin();
        activeStarts.put(name, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startComplete(String name) {
        ServiceStartEvent event = activeStarts.remove(name);
        if (event != null) {
            event.commit();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startFailed(String name, String reason) {
        ServiceStartEvent event = activeStarts.remove(name);
        if (event != null) {
            event.failed = true;
            event.failureReason = reason;
            event.commit();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopInitiated(String name) {
        ServiceStopEvent event = new ServiceStopEvent();
        event.serviceName = name;
        event.begin();
        activeStops.put(name, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopComplete(String name) {
        ServiceStopEvent event = activeStops.remove(name);
        if (event != null) {
            event.commit();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopFailed(String name, String reason) {
        ServiceStopEvent event = activeStops.remove(name);
        if (event != null) {
            event.failed = true;
            event.failureReason = reason;
            event.commit();
        }
    }

    /**
     * JFR event representing a Quarkus service startup lifecycle transition.
     */
    @Label("Service Start")
    @Category({ "Quarkus", "Service" })
    @Name("quarkus.ServiceStart")
    @Description("Quarkus service startup lifecycle transition")
    @StackTrace(false)
    public static class ServiceStartEvent extends Event {

        /**
         * The name/key of the service.
         */
        @Label("Service Name")
        public String serviceName;

        /**
         * Whether the service startup failed.
         */
        @Label("Failed")
        public boolean failed;

        /**
         * The failure reason/exception representation if failed.
         */
        @Label("Failure Reason")
        public String failureReason;
    }

    /**
     * JFR event representing a Quarkus service stop lifecycle transition.
     */
    @Label("Service Stop")
    @Category({ "Quarkus", "Service" })
    @Name("quarkus.ServiceStop")
    @Description("Quarkus service stop lifecycle transition")
    @StackTrace(false)
    public static class ServiceStopEvent extends Event {

        /**
         * The name/key of the service.
         */
        @Label("Service Name")
        public String serviceName;

        /**
         * Whether the service stop failed.
         */
        @Label("Failed")
        public boolean failed;

        /**
         * The failure reason/exception representation if failed.
         */
        @Label("Failure Reason")
        public String failureReason;
    }
}
