package io.quarkus.qute.debug;

import org.eclipse.lsp4j.debug.OutputEventArguments;

/**
 * Listener interface for receiving debugger events from the Qute debugger.
 * <p>
 * Implementations of this interface are notified when key events occur during
 * a debugging session, such as output being produced, threads being started or
 * stopped, execution being paused, or the debugger terminating.
 * </p>
 * <p>
 * This is typically used by the UI layer or other components that need to
 * react to changes in the debugging state.
 * </p>
 */
public interface DebuggerListener {

    /**
     * Called when the debuggee produces output.
     * <p>
     * This is typically used to display logs, console messages, or any other
     * textual output generated during the debugging session.
     * </p>
     *
     * @param args the {@link OutputEventArguments} containing the output details,
     *        such as the output text and category (e.g., stdout, stderr)
     */
    void output(OutputEventArguments args);

    /**
     * Called when the state of a thread changes, such as when a thread is started
     * or exited.
     *
     * @param event the {@link ThreadEvent} representing the change in thread state
     */
    void onThreadChanged(ThreadEvent event);

    /**
     * Called when the debuggee execution is stopped, usually due to hitting a breakpoint,
     * completing a step operation, or encountering an exception.
     *
     * @param event the {@link StoppedEvent} containing details about why the execution stopped
     */
    void onStopped(StoppedEvent event);

    /**
     * Called when the debugger session terminates completely.
     * <p>
     * After this method is invoked, no further events will be sent to the listener.
     * Implementations can use this callback to clean up resources or update the UI
     * to reflect the end of the debugging session.
     * </p>
     */
    void onTerminate();
}
