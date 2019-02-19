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

package io.quarkus.runtime;

/**
 * Event class that is fired on startup.
 *
 * This is fired on main method execution after all startup code has run,
 * so can be used to start threads etc in native image mode
 *
 * This event is observed as follows:
 *
 * <code><pre>
 *     void onStart(@Observes StartupEvent ev) {
 *         LOGGER.info("The application is starting...");
 *     }
 * </pre></code>
 *
 * The annotated method can access other injected beans.
 *
 *
 * TODO: make a real API.
 */
public class StartupEvent {
}
