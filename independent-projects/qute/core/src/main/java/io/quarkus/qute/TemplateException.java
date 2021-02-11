package io.quarkus.qute;

import io.quarkus.qute.TemplateNode.Origin;

public class TemplateException extends RuntimeException {

    private static final long serialVersionUID = 1336799943548973690L;

    private final Origin origin;

    public TemplateException(Throwable cause) {
        this(null, null, cause);
    }

    public TemplateException(String message) {
        this(null, message, null);
    }

    public TemplateException(Origin origin, String message) {
        this(origin, message, null);
    }

    public TemplateException(Origin origin, String message, Throwable cause) {
        super(message, cause);
        this.origin = origin;
    }

    public Origin getOrigin() {
        return origin;
    }

}
