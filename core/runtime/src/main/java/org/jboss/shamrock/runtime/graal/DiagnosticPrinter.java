package io.quarkus.runtime.graal;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Map;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.posix.headers.Pthread;

/**
 * A signal handler that prints diagnostic thread info to standard output.
 */
public final class DiagnosticPrinter {
    @TargetClass(className = "com.oracle.svm.core.posix.thread.PosixJavaThreads")
    @Platforms({ Platform.LINUX.class, Platform.DARWIN.class })
    static final class Target_PosixJavaThreads {
        @Alias
        static native Pthread.pthread_t getPthreadIdentifier(Thread thread);

        @Alias
        static native boolean hasThreadIdentifier(Thread thread);
    }

    public static void printDiagnostics(PrintStream w) {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        w.println(Instant.now().toString());
        w.println("Thread dump follows:");
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            w.println();
            final Thread thread = entry.getKey();
            final StackTraceElement[] stackTrace = entry.getValue();
            w.print('"');
            w.print(thread.getName());
            w.print("\" #");
            w.print(thread.getId());
            w.print(" ");
            if (thread.isDaemon())
                w.print("daemon ");
            w.print("prio=");
            w.print(thread.getPriority());
            w.print(" tid=");
            if (Target_PosixJavaThreads.hasThreadIdentifier(thread)) {
                final long nativeId = Target_PosixJavaThreads.getPthreadIdentifier(thread).rawValue();
                w.print("0x");
                w.println(Long.toHexString(nativeId));
            } else {
                w.println("(unknown)");
            }
            w.print("   java.lang.thread.State: ");
            w.println(thread.getState());
            for (StackTraceElement element : stackTrace) {
                w.print("\tat ");
                w.print(element.getClassName());
                w.print('.');
                w.print(element.getMethodName());
                w.print('(');
                // todo: "moduleName@version/"
                final String fileName = element.getFileName();
                w.print(fileName == null ? "unknown source" : fileName);
                final int lineNumber = element.getLineNumber();
                if (lineNumber > 0) {
                    w.print(':');
                    w.print(lineNumber);
                }
                w.println(')');
            }
        }
    }
}
