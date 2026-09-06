package io.quarkus.produi.runtime.diagnostics;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/**
 * Pure, read-only formatter that turns a JVM thread snapshot into a {@code jstack}-like text dump.
 * <p>
 * It renders only structural information already present in {@link ThreadInfo}: thread name, id, state, stack frames
 * (class/method/line), and lock/monitor identities. It never touches variable or field values, so a thread dump cannot
 * leak secrets - which is exactly why the thread dump is the only diagnostic action Prod UI allows. Keeping the logic
 * here (free of JMX access and CDI) makes it unit-testable in isolation.
 */
public final class ThreadDumpFormatter {

    private ThreadDumpFormatter() {
    }

    /**
     * Formats a full thread snapshot.
     *
     * @param threads the thread infos, e.g. from {@code ThreadMXBean.dumpAllThreads(true, true)} (may contain nulls)
     * @return a human-readable, multi-thread dump
     */
    public static String format(ThreadInfo[] threads) {
        StringBuilder sb = new StringBuilder();
        if (threads == null || threads.length == 0) {
            return "No threads.\n";
        }
        for (ThreadInfo thread : threads) {
            if (thread == null) {
                // A thread may have terminated between enumeration and inspection.
                continue;
            }
            appendThread(sb, thread);
            sb.append('\n');
        }
        return sb.toString();
    }

    private static void appendThread(StringBuilder sb, ThreadInfo thread) {
        sb.append('"').append(thread.getThreadName()).append('"')
                .append(" #").append(thread.getThreadId())
                .append(' ').append(thread.getThreadState());
        if (thread.getLockName() != null) {
            sb.append(" on ").append(thread.getLockName());
        }
        if (thread.getLockOwnerName() != null) {
            sb.append(" owned by \"").append(thread.getLockOwnerName())
                    .append("\" #").append(thread.getLockOwnerId());
        }
        if (thread.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (thread.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');

        StackTraceElement[] stack = thread.getStackTrace();
        MonitorInfo[] monitors = thread.getLockedMonitors();
        for (int i = 0; i < stack.length; i++) {
            sb.append("\tat ").append(stack[i]).append('\n');
            for (MonitorInfo monitor : monitors) {
                if (monitor.getLockedStackDepth() == i) {
                    sb.append("\t- locked ").append(monitor).append('\n');
                }
            }
        }

        LockInfo[] synchronizers = thread.getLockedSynchronizers();
        if (synchronizers.length > 0) {
            sb.append("\n\tLocked ownable synchronizers:\n");
            for (LockInfo synchronizer : synchronizers) {
                sb.append("\t- ").append(synchronizer).append('\n');
            }
        }
    }
}
