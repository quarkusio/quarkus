package io.quarkus.jfr.runtime.internal.runtime;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.jfr.runtime.internal.config.JfrRuntimeConfig;
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

    private Runnable runtimeEventTask;
    private Runnable applicationEventTask;
    private Runnable extensionEventTask;

    public void enable() {
        FlightRecorder.register(QuarkusRuntimeEvent.class);
        FlightRecorder.register(QuarkusApplicationEvent.class);
        FlightRecorder.register(ExtensionEvent.class);

        runtimeEventTask = new RuntimeEventTask(quarkusRuntimeInfo.version(), quarkusRuntimeInfo.imageMode(),
                quarkusRuntimeInfo.profiles());
        applicationEventTask = new ApplicationEventTask(applicationConfig.name().get(), applicationConfig.version().get());
        extensionEventTask = new ExtensionEventTask(quarkusRuntimeInfo.features());

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

        private final String version;
        private final String imageMode;
        private final String profiles;

        public RuntimeEventTask(String version, String imageMode, String profiles) {
            this.version = version;
            this.imageMode = imageMode;
            this.profiles = profiles;
        }

        @Override
        public void run() {
            QuarkusRuntimeEvent event = new QuarkusRuntimeEvent();
            if (event.shouldCommit()) {
                event.setVersion(version);
                event.setImageMode(imageMode);
                event.setProfiles(profiles);
                event.commit();
            }
        }
    }

    class ApplicationEventTask implements Runnable {

        private final String name;
        private final String version;

        public ApplicationEventTask(String name, String version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public void run() {
            QuarkusApplicationEvent event = new QuarkusApplicationEvent();
            if (event.shouldCommit()) {
                event.setName(name);
                event.setVersion(version);
                event.commit();
            }
        }
    }

    class ExtensionEventTask implements Runnable {

        private final List<String> features;

        public ExtensionEventTask(List<String> features) {
            this.features = features;
        }

        @Override
        public void run() {
            for (String feature : features) {
                ExtensionEvent event = new ExtensionEvent();
                if (event.shouldCommit()) {
                    event.setName(feature);
                    event.commit();
                }
            }
        }
    }
}
