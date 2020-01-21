package io.quarkus.scheduler.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Singleton;

import com.cronutils.model.CronType;

/**
 *
 * @author Martin Kouba
 */
@Singleton
public class SchedulerSupport {

    private volatile ExecutorService executor;
    private volatile CronType cronType;
    private volatile List<ScheduledMethodMetadata> scheduledMethods;

    void initialize(SchedulerConfig config, List<ScheduledMethodMetadata> scheduledMethods, ExecutorService executor) {
        this.cronType = config.cronType;
        this.scheduledMethods = scheduledMethods;
        this.executor = executor;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public CronType getCronType() {
        return cronType;
    }

    public List<ScheduledMethodMetadata> getScheduledMethods() {
        return scheduledMethods;
    }

    @SuppressWarnings("unchecked")
    public ScheduledInvoker createInvoker(String invokerClassName) {
        try {
            Class<? extends ScheduledInvoker> invokerClazz = (Class<? extends ScheduledInvoker>) Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(invokerClassName);
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
