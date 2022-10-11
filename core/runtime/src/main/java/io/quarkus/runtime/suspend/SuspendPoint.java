package io.quarkus.runtime.suspend;

import java.util.concurrent.CountDownLatch;

/**
 * Utilities for supporting generalized post-startup suspend at run time using CRaC or a similar API.
 */
public final class SuspendPoint {
    private SuspendPoint() {
    }

    private static final CountDownLatch waitForStartup = new CountDownLatch(1);
    private static final CountDownLatch waitForResume = new CountDownLatch(1);
    private static final CountDownLatch waitForComplete = new CountDownLatch(1);

    /**
     * Indicate that startup is complete and that it is safe to suspend the VM.
     * Called from the main thread.
     */
    public static void readyToSuspend() {
        waitForStartup.countDown();
        awaitUninterruptibly(waitForResume);
    }

    /**
     * Indicate that the environment is ready to suspend the VM once startup completes.
     * Called from a dedicated suspend/resume control thread (i.e. not the main thread).
     */
    public static void suspend() {
        awaitUninterruptibly(waitForStartup);
    }

    /**
     * Indicate that the VM has been unsuspended and can now resume execution.
     * Called from a dedicated suspend/resume control thread (i.e. not the main thread).
     */
    public static void resume() {
        waitForResume.countDown();
        awaitUninterruptibly(waitForComplete);
    }

    /**
     * Indicate that the environment is ready to service requests.
     */
    public static void readyForRequests() {
        waitForComplete.countDown();
    }

    /**
     * An implementation of {@code org.crac.Resource} which uses this API to suspend/restore.
     */
    public static final org.crac.Resource ORG_CRAC_RESOURCE = new org.crac.Resource() {
        public void beforeCheckpoint(final org.crac.Context<? extends org.crac.Resource> context) {
            suspend();
        }

        public void afterRestore(final org.crac.Context<? extends org.crac.Resource> context) {
            resume();
        }
    };

//    /**
//     * An implementation of {@code jdk.crac.Resource} which uses this API to suspend/restore.
//     */
//    public static final jdk.crac.Resource JDK_CRAC_RESOURCE = new jdk.crac.Resource() {
//        public void beforeCheckpoint(final jdk.crac.Context<? extends jdk.crac.Resource> context) {
//            suspend();
//        }
//
//        public void afterRestore(final jdk.crac.Context<? extends jdk.crac.Resource> context) {
//            resume();
//        }
//    };

    private static void awaitUninterruptibly(final CountDownLatch cdl) {
        boolean intr = false;
        try {
            for (; ; )
                try {
                    cdl.await();
                    break;
                } catch (InterruptedException e) {
                    intr = true;
                }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
