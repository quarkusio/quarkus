/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.scheduler.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.scheduler.api.Scheduled;

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
