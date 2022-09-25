package io.quarkus.qute.debug;

public class DebuggerStoppedException extends DebuggerException {

    public DebuggerStoppedException(Throwable e) {
        super(e);
    }

    public DebuggerStoppedException() {
        super();
    }

}
