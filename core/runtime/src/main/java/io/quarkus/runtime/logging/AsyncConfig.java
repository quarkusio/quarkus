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

package io.quarkus.runtime.logging;

import org.jboss.logmanager.handlers.AsyncHandler.OverflowAction;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AsyncConfig {

    /**
     * Indicates whether to log asynchronously
     */
    @ConfigItem(name = ConfigItem.PARENT)
    boolean enable;
    /**
     * The queue length to use before flushing writing
     */
    @ConfigItem(defaultValue = "512")
    int queueLength;

    /**
     * Determine whether to block the publisher (rather than drop the message) when the queue is full
     */
    @ConfigItem(defaultValue = "BLOCK")
    OverflowAction overflow;
}
