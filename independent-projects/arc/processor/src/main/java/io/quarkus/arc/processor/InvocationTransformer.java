package io.quarkus.arc.processor;

final class InvocationTransformer {
    final InvocationTransformerKind kind;
    final Class<?> clazz;
    final String method;

    InvocationTransformer(InvocationTransformerKind kind, Class<?> clazz, String method) {
        this.kind = kind;
        this.clazz = clazz;
        this.method = method;
    }

    @Override
    public String toString() {
        String kind = switch (this.kind) {
            case INSTANCE -> "target instance transformer ";
            case ARGUMENT -> "argument transformer ";
            case RETURN_VALUE -> "return value transformer ";
            case EXCEPTION -> "exception transformer ";
            case WRAPPER -> "invocation wrapper ";
        };
        return kind + clazz.getName() + "#" + method;
    }

    public boolean isInputTransformer() {
        return kind == InvocationTransformerKind.INSTANCE || kind == InvocationTransformerKind.ARGUMENT;
    }

    public boolean isOutputTransformer() {
        return kind == InvocationTransformerKind.RETURN_VALUE || kind == InvocationTransformerKind.EXCEPTION;
    }
}
