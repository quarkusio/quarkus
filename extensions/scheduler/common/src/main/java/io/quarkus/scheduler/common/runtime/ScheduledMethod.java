package io.quarkus.scheduler.common.runtime;

import java.util.List;

import io.quarkus.scheduler.Scheduled;

/**
 * Scheduled method metadata.
 */
public interface ScheduledMethod {

    String getInvokerClassName();

    String getDeclaringClassName();

    String getMethodName();

    List<Scheduled> getSchedules();

    default String getMethodDescription() {
        return getDeclaringClassName() + "#" + getMethodName();
    }

}
