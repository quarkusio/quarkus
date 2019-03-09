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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

public class StartupContext implements Closeable {

    private static final Logger LOG = Logger.getLogger(StartupContext.class);

    private final Map<String, Object> values = new HashMap<>();
    private final List<Runnable> shutdownTasks = new ArrayList<>();
    private final ShutdownContext shutdownContext = new ShutdownContext() {
        @Override
        public void addShutdownTask(Runnable runnable) {
            shutdownTasks.add(runnable);
        }
    };

    public StartupContext() {
        values.put(ShutdownContext.class.getName(), shutdownContext);
    }

    public void putValue(String name, Object value) {
        values.put(name, value);
    }

    public Object getValue(String name) {
        return values.get(name);
    }

    @Override
    public void close() {
        List<Runnable> toClose = new ArrayList<>(shutdownTasks);
        Collections.reverse(toClose);
        for (Runnable r : toClose) {
            try {
                r.run();
            } catch (Throwable e) {
                LOG.error("Running a shutdown task failed", e);
            }
        }
        shutdownTasks.clear();
    }
}
