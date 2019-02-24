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

package io.quarkus.arc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Martin Kouba
 */
public final class Arc {

    private static final AtomicReference<ArcContainerImpl> INSTANCE = new AtomicReference<>();

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static ArcContainer initialize() {
        if (INITIALIZED.compareAndSet(false, true)) {
            ArcContainerImpl container = new ArcContainerImpl();
            INSTANCE.set(container);
            container.init();
        }
        return container();
    }

    /**
     *
     * @return the container instance
     */
    public static ArcContainer container() {
        return INSTANCE.get();
    }

    public static void shutdown() {
        if (INSTANCE.get() != null) {
            synchronized (INSTANCE) {
                if (INSTANCE.get() != null) {
                    INSTANCE.get().shutdown();
                    INSTANCE.set(null);
                    INITIALIZED.set(false);
                }
            }
        }
    }

}
