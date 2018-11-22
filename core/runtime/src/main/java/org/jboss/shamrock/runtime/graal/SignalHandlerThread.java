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

package org.jboss.shamrock.runtime.graal;

import java.util.concurrent.locks.LockSupport;

import org.graalvm.nativeimage.RuntimeClassInitialization;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * @see RuntimeClassInitialization#delayClassInitialization(java.lang.Class[])
 */
@SuppressWarnings("unused")
public final class SignalHandlerThread extends Thread {
    private static final SignalHandlerThread instance;

    static {
        instance = new SignalHandlerThread(currentThread().getThreadGroup());
        instance.start();
    }

    private SignalHandlerThread(final ThreadGroup group) {
        // use a small stack
        super(group, null, "Signal Listener", 0x10000L);
        setDaemon(true);
    }

    public void interrupt() {
        // no op
    }

    public void run() {
        final SignalHandler handler = new SignalHandler() {
            public void handle(final Signal signal) {
                System.exit(0);
            }
        };
        Signal.handle(new Signal("INT"), handler);
        Signal.handle(new Signal("TERM"), handler);
        for (;;) {
            Thread.interrupted();
            LockSupport.park();
        }
    }
}
