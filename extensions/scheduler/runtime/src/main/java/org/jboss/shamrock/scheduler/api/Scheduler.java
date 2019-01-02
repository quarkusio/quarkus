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
package org.jboss.shamrock.scheduler.api;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

/**
 * The container provides a built-in bean with bean type {@link Scheduler}, scope {@link ApplicationScoped}, and qualifier {@link Default}.
 *
 * @author Martin Kouba
 */
public interface Scheduler {

    /**
     * Pause all jobs.
     */
    void pause();

    /**
     * Resume all jobs.
     */
    void resume();

    /**
     * Start a one-shot timer to fire after the specified delay.
     *
     * @param delay The delay in milliseconds,
     * @param action The action to run
     */
    void startTimer(long delay, Runnable action);

}
