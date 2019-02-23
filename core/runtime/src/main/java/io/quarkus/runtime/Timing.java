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

import java.math.BigDecimal;

import org.jboss.logging.Logger;

/**
 * Class that is responsible for printing out timing results.
 * <p>
 * It is modified on substrate by {@link io.quarkus.runtime.graal.TimingReplacement}, in that mainStarted it rewritten to
 * actually update the start time.
 */
public class Timing {

    private static volatile long bootStartTime = -1;

    private static volatile long bootStopTime = -1;

    public static void staticInitStarted() {
        if (bootStartTime < 0) {
            bootStartTime = System.nanoTime();
        }
    }

    public static void staticInitStopped() {
        if (bootStopTime < 0) {
            bootStopTime = System.nanoTime();
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
        final long bootTimeNanoSeconds = System.nanoTime() - bootStartTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        //Use a BigDecimal so we can render in seconds with 3 digits precision, as requested:
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(bootTimeNanoSeconds);
        logger.infof("Quarkus %s started in %ss. %s", version, secondsRepresentation, httpServer);
        logger.infof("Installed features: [%s]", features);
    }

    public static void printStopTime() {
        final long stopTimeNanoSeconds = System.nanoTime() - bootStopTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(stopTimeNanoSeconds);
        logger.infof("Quarkus stopped in %ss", secondsRepresentation);
    }

    private static BigDecimal convertToBigDecimalSeconds(final long timeNanoSeconds) {
        final BigDecimal secondsRepresentation=BigDecimal.valueOf(timeNanoSeconds) // As nanoseconds
        .divide(BigDecimal.valueOf(1_000_000),BigDecimal.ROUND_HALF_UP) // Convert to milliseconds, discard remaining digits while rounding
        .divide(BigDecimal.valueOf(1_000),3,BigDecimal.ROUND_HALF_UP); // Convert to seconds, while preserving 3 digits
        return secondsRepresentation;
    }

}
