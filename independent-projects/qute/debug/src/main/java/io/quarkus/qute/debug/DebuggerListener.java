package io.quarkus.qute.debug;

import org.eclipse.lsp4j.debug.OutputEventArguments;

public interface DebuggerListener {

    void output(OutputEventArguments args);

    void onThreadChanged(ThreadEvent event);

    void onStopped(StoppedEvent event);

    void onTerminate();

}
