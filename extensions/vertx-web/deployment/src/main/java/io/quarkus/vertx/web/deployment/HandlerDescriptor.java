package io.quarkus.vertx.web.deployment;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.vertx.core.buffer.Buffer;

/**
 * Describe a request handler.
 */
public class HandlerDescriptor {

    private final MethodInfo method;

    public HandlerDescriptor(MethodInfo method) {
        this.method = method;
    }

    public Type getReturnType() {
        return method.returnType();
    }

    public boolean isReturningVoid() {
        return method.returnType().kind().equals(Type.Kind.VOID);
    }

    public boolean isReturningUni() {
        return method.returnType().name().equals(DotNames.UNI);
    }

    public boolean isReturningMulti() {
        return method.returnType().name().equals(DotNames.MULTI);
    }

    public Type getContentType() {
        if (isReturningVoid()) {
            return null;
        }
        if (isReturningUni()) {
            return getReturnType().asParameterizedType().arguments().get(0);
        }
        if (isReturningMulti()) {
            return getReturnType().asParameterizedType().arguments().get(0);
        }
        return getReturnType();
    }

    public boolean isContentTypeString() {
        Type type = getContentType();
        if (type == null) {
            return false;
        }
        return type.name().equals(io.quarkus.arc.processor.DotNames.STRING);
    }

    public boolean isContentTypeBuffer() {
        Type type = getContentType();
        if (type == null) {
            return false;
        }
        return type.name().equals(DotNames.BUFFER);
    }

    public boolean isContentTypeRxBuffer() {
        Type type = getContentType();
        if (type == null) {
            return false;
        }
        return type.name()
                .equals(DotName.createSimple(io.vertx.reactivex.core.buffer.Buffer.class.getName()));
    }

    public boolean isContentTypeMutinyBuffer() {
        Type type = getContentType();
        if (type == null) {
            return false;
        }
        return type.name().equals(DotName.createSimple(io.vertx.mutiny.core.buffer.Buffer.class.getName()));
    }

}
