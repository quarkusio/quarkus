/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.shamrock.runtime.graal;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 */
public class ShutdownHookThread extends Thread {
    private final AtomicBoolean shutdownFlag;
    private final Thread mainThread;

    public ShutdownHookThread(final AtomicBoolean shutdownFlag, Thread mainThread) {
        super("Shutdown thread");
        this.shutdownFlag = shutdownFlag;
        this.mainThread = mainThread;
    }

    public void run() {
        shutdownFlag.set(true);
        LockSupport.unpark(mainThread);
    }

    public String toString() {
        return getName();
    }
}
