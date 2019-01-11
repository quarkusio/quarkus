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

package org.jboss.shamrock.runtime;

import org.jboss.logging.Logger;

/**
 * Class that is responsible for printing out timing results.
 * <p>
 * It is modified on substrate by {@link org.jboss.shamrock.runtime.graal.TimingReplacement}, in that mainStarted it rewritten to
 * actually update the start time.
 */
public class Timing {

    private static volatile long bootStartTime = -1;

    public static void staticInitStarted() {
        if(bootStartTime < 0) {
            bootStartTime = System.nanoTime();
        }
    }

    /**
     * This method is replaced by substrate
     */
    public static void mainStarted() {
    }

    public static void restart() {
        bootStartTime = System.nanoTime();
    }

    public static void printStartupTime(String version, String features, String httpServer) {
        final long time = System.nanoTime() - bootStartTime + 500;
        Logger.getLogger("org.jboss.shamrock").infof("Shamrock %s started in %d.%03dms. %s", version, Long.valueOf(time / 1_000_000), Long.valueOf(time % 1_000_000 / 1_000), httpServer);
        Logger.getLogger("org.jboss.shamrock").infof("Installed features: [%s]", features);
    }

}
