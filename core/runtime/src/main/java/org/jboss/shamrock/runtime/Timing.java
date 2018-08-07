package org.jboss.shamrock.runtime;

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
            bootStartTime = System.currentTimeMillis();
        }
    }

    /**
     * This method is replaced by substrate
     */
    public static void mainStarted() {
    }

    public static void restart() {
        bootStartTime = System.currentTimeMillis();
    }

    public static void printStartupTime() {
        System.out.println("Shamrock started in " + (System.currentTimeMillis() - bootStartTime) + "ms");
    }

}
