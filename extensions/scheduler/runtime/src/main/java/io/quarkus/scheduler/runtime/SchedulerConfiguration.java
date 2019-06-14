package io.quarkus.scheduler.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.scheduler.Scheduled;

/**
 *
 * @author Martin Kouba
 */
@ApplicationScoped
public class SchedulerConfiguration {

    private final Map<String, List<Scheduled>> schedules = new ConcurrentHashMap<>();

    private final Map<String, String> descriptions = new ConcurrentHashMap<>();

    void register(String invokerClassName, String description, List<Scheduled> schedules) {
        this.schedules.put(invokerClassName, schedules);
        this.descriptions.put(invokerClassName, description);
    }

    Map<String, List<Scheduled>> getSchedules() {
        return schedules;
    }

    String getDescription(String invokerClassName) {
        return descriptions.get(invokerClassName);
    }

    @SuppressWarnings("unchecked")
    ScheduledInvoker createInvoker(String invokerClassName) {
        try {
            Class<? extends ScheduledInvoker> invokerClazz = (Class<? extends ScheduledInvoker>) Thread.currentThread()
                    .getContextClassLoader().loadClass(invokerClassName);
            return invokerClazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new IllegalStateException("Unable to create invoker: " + invokerClassName, e);
        }
    }

    public static String getConfigProperty(String val) {
        return val.substring(1, val.length() - 1);
    }

    public static boolean isConfigValue(String val) {
        val = val.trim();
        return val.startsWith("{") && val.endsWith("}");
    }

}
