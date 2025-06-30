package io.quarkus.jfr.runtime.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.jfr.runtime.config.JfrRuntimeConfig;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jdk.jfr.FlightRecorder;

@ApplicationScoped
public class JfrRuntimeBean {

    @Inject
    JfrRuntimeConfig jfrRuntimeConfig;

    @Inject
    QuarkusRuntimeInfo quarkusRuntimeInfo;

    @Inject
    ApplicationConfig applicationConfig;

    private final Runnable runtimeEventTask = new RuntimeEventTask();
    private final Runnable applicationEventTask = new ApplicationEventTask();
    private final Runnable extensionEventTask = new ExtensionEventTask();

    public void enable() {
        FlightRecorder.register(QuarkusRuntimeEvent.class);
        FlightRecorder.register(QuarkusApplicationEvent.class);
        FlightRecorder.register(ExtensionEvent.class);
        FlightRecorder.addPeriodicEvent(QuarkusRuntimeEvent.class, runtimeEventTask);
        FlightRecorder.addPeriodicEvent(QuarkusApplicationEvent.class, applicationEventTask);
        FlightRecorder.addPeriodicEvent(ExtensionEvent.class, extensionEventTask);
    }

    public void disable() {
        FlightRecorder.removePeriodicEvent(runtimeEventTask);
        FlightRecorder.removePeriodicEvent(applicationEventTask);
        FlightRecorder.removePeriodicEvent(extensionEventTask);
        FlightRecorder.unregister(QuarkusRuntimeEvent.class);
        FlightRecorder.unregister(QuarkusApplicationEvent.class);
        FlightRecorder.unregister(ExtensionEvent.class);
    }

    public void onStart(@Observes StartupEvent ev) {
        if (jfrRuntimeConfig.runtimeEnabled()) {
            this.enable();
        }
    }

    public void onStop(@Observes ShutdownEvent ev) {
        if (jfrRuntimeConfig.runtimeEnabled()) {
            this.disable();
        }
    }

    class RuntimeEventTask implements Runnable {
        @Override
        public void run() {
            QuarkusRuntimeEvent event = new QuarkusRuntimeEvent();
            if (event.shouldCommit()) {
                event.setVersion(quarkusRuntimeInfo.version());
                event.setImageMode(quarkusRuntimeInfo.imageMode());
                event.setProfiles(quarkusRuntimeInfo.profiles());
                event.commit();
            }
        }
    }

    class ApplicationEventTask implements Runnable {
        @Override
        public void run() {
            QuarkusApplicationEvent event = new QuarkusApplicationEvent();
            if (event.shouldCommit()) {
                event.setName(applicationConfig.name().get());
                event.setVersion(applicationConfig.version().get());
                event.commit();
            }
        }
    }

    class ExtensionEventTask implements Runnable {
        @Override
        public void run() {
            for (String feature : quarkusRuntimeInfo.features()) {
                ExtensionEvent event = new ExtensionEvent();
                if (event.shouldCommit()) {
                    event.setName(feature);
                    event.commit();
                }
            }
        }
    }
}
