package io.quarkus.micrometer.runtime.binder.virtualthreads;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.util.JavaVersionUtil;

/**
 * A component collecting metrics about virtual threads.
 * It will be only available when the virtual threads are enabled (Java 21+).
 * <p>
 * Note that metrics are collected using JFR events.
 */
@ApplicationScoped
public class VirtualThreadCollector {

    private static final String VIRTUAL_THREAD_BINDER_CLASSNAME = "io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics";
    private static final Logger LOGGER = Logger.getLogger(VirtualThreadCollector.class);

    final MeterRegistry registry = Metrics.globalRegistry;

    private final boolean enabled;
    private final MeterBinder binder;
    private final List<Tag> tags;

    @Inject
    public VirtualThreadCollector(MicrometerConfig mc) {
        var config = mc.binder().virtualThreads();
        this.enabled = JavaVersionUtil.isJava21OrHigher() && config.enabled().orElse(true);
        MeterBinder instantiated = null;
        if (enabled) {
            if (config.tags().isPresent()) {
                List<String> list = config.tags().get();
                this.tags = list.stream().map(this::createTagFromEntry).collect(Collectors.toList());
            } else {
                this.tags = List.of();
            }
            try {
                instantiated = instantiate(tags);
            } catch (Exception e) {
                LOGGER.warnf(e, "Failed to instantiate " + VIRTUAL_THREAD_BINDER_CLASSNAME);
            }
        } else {
            this.tags = List.of();
        }
        this.binder = instantiated;
    }

    /**
     * Use reflection to avoid calling a class touching Java 21+ APIs.
     *
     * @param tags the tags.
     * @return the binder, {@code null} if the instantiation failed.
     */
    public MeterBinder instantiate(List<Tag> tags) {
        try {
            Class<?> clazz = Class.forName(VIRTUAL_THREAD_BINDER_CLASSNAME);
            return (MeterBinder) clazz.getDeclaredConstructor(Iterable.class).newInstance(tags);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate " + VIRTUAL_THREAD_BINDER_CLASSNAME, e);
        }
    }

    private Tag createTagFromEntry(String entry) {
        String[] parts = entry.trim().split("=");
        if (parts.length == 2) {
            return Tag.of(parts[0], parts[1]);
        } else {
            throw new IllegalStateException("Invalid tag: " + entry + " (expected key=value)");
        }
    }

    public MeterBinder getBinder() {
        return binder;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void init(@Observes StartupEvent event) {
        if (enabled && binder != null) {
            binder.bindTo(registry);
        }
    }

    public void close(@Observes ShutdownEvent event) {
        if (binder instanceof Closeable) {
            try {
                ((Closeable) binder).close();
            } catch (IOException e) {
                LOGGER.warnf(e, "Failed to close " + VIRTUAL_THREAD_BINDER_CLASSNAME);
            }
        }
    }

}
