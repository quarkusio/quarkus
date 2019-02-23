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

package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.ShutdownContext;

/**
 * A build item that can be used to register shutdown tasks in runtime templates
 */
public final class ShutdownContextBuildItem extends SimpleBuildItem
        implements ShutdownContext, BytecodeRecorderImpl.ReturnedProxy {
    @Override
    public String __returned$proxy$key() {
        return ShutdownContext.class.getName();
    }

    @Override
    public boolean __static$$init() {
        return true;
    }

    @Override
    public void addShutdownTask(Runnable runnable) {
        throw new IllegalStateException();
    }
}
