package io.quarkus.produi.runtime.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

import org.junit.jupiter.api.Test;

class ThreadDumpFormatterTest {

    @Test
    void formatsRealThreadSnapshot() {
        ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        String dump = ThreadDumpFormatter.format(threads);

        // The running test thread must appear, with a stack frame and this test's own class/method.
        String currentThread = Thread.currentThread().getName();
        assertThat(dump)
                .contains('"' + currentThread + '"')
                .contains("\tat ")
                .contains(ThreadDumpFormatterTest.class.getName() + ".formatsRealThreadSnapshot");
    }

    @Test
    void nullAndEmptyAreHandled() {
        assertThat(ThreadDumpFormatter.format(null)).isEqualTo("No threads.\n");
        assertThat(ThreadDumpFormatter.format(new ThreadInfo[0])).isEqualTo("No threads.\n");
    }

    @Test
    void nullEntriesAreSkipped() {
        ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
        ThreadInfo[] withNull = new ThreadInfo[threads.length + 1];
        System.arraycopy(threads, 0, withNull, 0, threads.length);
        withNull[threads.length] = null;

        // A null entry (a thread that terminated mid-enumeration) must not blow up formatting.
        String dump = ThreadDumpFormatter.format(withNull);
        assertThat(dump).contains('"' + Thread.currentThread().getName() + '"');
    }
}
