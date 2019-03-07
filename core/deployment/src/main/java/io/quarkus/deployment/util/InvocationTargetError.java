package io.quarkus.deployment.util;

public class InvocationTargetError extends InstantiationError {
    private static final long serialVersionUID = 8514644790077971513L;

    InvocationTargetError(String message) {
        super(message);
    }
}
