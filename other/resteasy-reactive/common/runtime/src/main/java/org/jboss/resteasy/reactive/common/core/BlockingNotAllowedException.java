package org.jboss.resteasy.reactive.common.core;

public class BlockingNotAllowedException extends IllegalStateException {

    public BlockingNotAllowedException() {
        super();
    }

    public BlockingNotAllowedException(String s) {
        super(s);
    }

    public BlockingNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockingNotAllowedException(Throwable cause) {
        super(cause);
    }
}
