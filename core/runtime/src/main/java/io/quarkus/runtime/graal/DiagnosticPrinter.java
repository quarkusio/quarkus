package io.quarkus.runtime.graal;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Map;

/**
 * A signal handler that prints diagnostic thread info to standard output.
 */
public final class DiagnosticPrinter {
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
