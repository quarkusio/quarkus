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

    public static void printStartupTime() {
        final long time = System.nanoTime() - bootStartTime + 500;
        Logger.getLogger("org.jboss.shamrock").infof("Shamrock started in %d.%03dms", Long.valueOf(time / 1_000_000), Long.valueOf(time % 1_000_000 / 1_000));
    }

}
