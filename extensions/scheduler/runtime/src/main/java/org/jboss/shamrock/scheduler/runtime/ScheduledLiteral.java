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
package org.jboss.shamrock.scheduler.runtime;

import java.util.concurrent.TimeUnit;

import javax.enterprise.util.AnnotationLiteral;

import org.jboss.shamrock.scheduler.api.Scheduled;

/**
 * Inline instantiation of {@link Scheduled}.
 *
 * @author Martin Kouba
 */
public final class ScheduledLiteral extends AnnotationLiteral<Scheduled> implements Scheduled {

    static Builder builder() {
        return new Builder();
    }

    private static final long serialVersionUID = 1L;

    private final String cron;

    private final String every;

    private final long delay;

    private final TimeUnit delayUnit;

    public ScheduledLiteral(String cron, String every, long delay, TimeUnit delayUnit) {
        this.cron = cron;
        this.every = every;
        this.delay = delay;
        this.delayUnit = delayUnit;
    }

    @Override
    public String cron() {
        return cron;
    }

    @Override
    public String every() {
        return every;
    }

    @Override
    public long delay() {
        return delay;
    }

    @Override
    public TimeUnit delayUnit() {
        return delayUnit;
    }

    static class Builder {

        private String cron = "";
        private String every = "";
        private long delay = 0;
        private TimeUnit delayUnit = TimeUnit.MINUTES;

        private Builder() {
        }

        Builder with(String name, Object value) {
            switch (name) {
                case "cron":
                    this.cron = value.toString();
                    break;
                case "every":
                    this.every = value.toString();
                    break;
                case "delay":
                    this.delay = (long) value;
                    break;
                case "delayUnit":
                    this.delayUnit = TimeUnit.valueOf(value.toString());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown annotation member: " + name);
            }
            return this;
        }

        Scheduled build() {
            return new ScheduledLiteral(cron, every, delay, delayUnit);
        }

    }

    public static boolean isConfigValue(String val) {
    	val = val.trim();
    	return val.startsWith("{") && val.endsWith("}");
    }
    
    public static String getConfigProperty(String val) {
        return val.substring(1, val.length() - 1);
    }

}