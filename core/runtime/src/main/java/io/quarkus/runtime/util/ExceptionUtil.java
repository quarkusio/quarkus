package io.quarkus.runtime.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import io.quarkus.dev.console.CurrentAppExceptionHighlighter;

/**
 *
 */
public class ExceptionUtil {

    /**
     * Returns the string representation of the stacktrace of the passed {@code exception}
     *
     * @param exception
     * @return
     */
    public static String generateStackTrace(final Throwable exception) {
        if (exception == null) {
            return null;
        }
        final StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));

        return stringWriter.toString().trim();
    }

    /**
     * Returns a "upside down" stacktrace of the {@code exception} with the root
     * cause showing up first in the stacktrace.
     * <em>Note:</em> This is a relatively expensive method because it creates additional
     * exceptions and manipulates their stacktraces. Care should be taken to determine whether
     * usage of this method is necessary.
     *
     * @param exception The exception
     * @return
     */
    public static String rootCauseFirstStackTrace(final Throwable exception) {
        if (exception == null) {
            return null;
        }
        AutoCloseable closeable = null;
        try {
            BiFunction<Throwable, CurrentAppExceptionHighlighter.Target, AutoCloseable> formatter = CurrentAppExceptionHighlighter.THROWABLE_FORMATTER;
            if (formatter != null) {
                formatter.apply(exception, CurrentAppExceptionHighlighter.Target.HTML);
            }
            // create an exception chain with the root cause being at element 0
            final List<Throwable> exceptionChain = new ArrayList<>();
            Throwable curr = exception;
            while (curr != null) {
                exceptionChain.add(0, curr);
                curr = curr.getCause();
            }
            Throwable prevStrippedCause = null;
            Throwable modifiedRoot = null;
            // We reverse the stacktrace as follows:
            // - Iterate the exception chain that we created, which has the root cause at element 0
            // - for each exception in this chain
            //      - create a new "copy" C1 of that exception
            //      - create a new copy C2 of the "next" exception in the chain with its cause stripped off
            //      - C1.initCause(C2)
            //      - keep track of the copy C1 of the first element in the exception chain. That C1, lets call
            //        it RC1, will be the modified representation of the root cause on which if printStackTrace()
            //        is called, then it will end up printing stacktrace in reverse order (because of the way we
            //        fiddled around with its causes and other details)
            // - Finally, replace the occurrences of "Caused by:" string the in the stacktrace to "Resulted in:"
            //   to better phrase the reverse stacktrace representation.
            for (int i = 0; i < exceptionChain.size(); i++) {
                final Throwable x = prevStrippedCause == null ? stripCause(exceptionChain.get(0)) : prevStrippedCause;
                if (i != exceptionChain.size() - 1) {
                    final Throwable strippedCause = stripCause(exceptionChain.get(i + 1));
                    x.initCause(strippedCause);
                    prevStrippedCause = strippedCause;
                }
                if (i == 0) {
                    modifiedRoot = x;
                }
            }
            return generateStackTrace(modifiedRoot).replace("Caused by:", "Resulted in:");
        } finally {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public static Throwable getRootCause(Throwable exception) {
        final List<Throwable> chain = new ArrayList<>();
        Throwable curr = exception;
        while (curr != null && !chain.contains(curr)) {
            chain.add(curr);
            curr = curr.getCause();
        }
        return chain.isEmpty() ? null : chain.get(chain.size() - 1);
    }

    /**
     * Creates and returns a new {@link Throwable} which has the following characteristics:
     * <ul>
     * <li>The {@code cause} of the Throwable hasn't yet been {@link Throwable#initCause(Throwable) inited}
     * and thus can be "inited" later on if needed
     * </li>
     * <li>
     * The stacktrace elements of the Throwable have been set to the stacktrace elements of the passed
     * {@code t}. That way, any call to {@link Throwable#printStackTrace(PrintStream)} for example
     * will print the stacktrace of the passed {@code t}
     * </li>
     * </ul>
     *
     * @param t The exception
     * @return
     */
    private static Throwable stripCause(final Throwable t) {
        final Throwable stripped = delegatingToStringThrowable(t);
        stripped.setStackTrace(t.getStackTrace());
        return stripped;
    }

    /**
     * Creates and returns a new {@link Throwable} whose {@link Throwable#toString()} has been
     * overridden to call the {@code toString()} method of the passed {@code t}.
     *
     * @param t The exception
     * @return
     */
    private static Throwable delegatingToStringThrowable(final Throwable t) {
        return new Throwable() {
            @Override
            public String toString() {
                return t.toString();
            }
        };
    }
}
