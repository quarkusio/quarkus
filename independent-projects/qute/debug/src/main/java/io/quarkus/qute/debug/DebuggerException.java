package io.quarkus.qute.debug;

public class DebuggerException extends RuntimeException {

    public DebuggerException(Throwable e) {
        super(e);
    }

    public DebuggerException() {
    }

}
