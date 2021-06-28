package io.quarkus.qute;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 *
 * @see ReflectionValueResolver
 */
final class MethodsCandidate implements AccessorCandidate {

    private final List<Method> methods;

    public MethodsCandidate(List<Method> methods) {
        this.methods = methods;
    }

    @Override
    public ValueAccessor getAccessor(EvalContext context) {
        EvaluatedParams params = EvaluatedParams.evaluate(context);
        return new ValueAccessor() {
            @Override
            public CompletionStage<Object> getValue(Object instance) {
                CompletableFuture<Object> result = new CompletableFuture<>();
                params.stage.whenComplete((r, t) -> {
                    if (t != null) {
                        result.completeExceptionally(t);
                    } else {
                        for (Method method : methods) {
                            try {
                                if (params.parameterTypesMatch(method.isVarArgs(), method.getParameterTypes())) {
                                    Object[] args = new Object[method.getParameterCount()];
                                    for (int i = 0; i < args.length; i++) {
                                        if (method.isVarArgs() && (i == args.length - 1)) {
                                            Class<?> lastParam = method.getParameterTypes()[i];
                                            args[i] = params.getVarargsResults(method.getParameterCount(),
                                                    lastParam.getComponentType());
                                        } else {
                                            args[i] = params.getResult(i);
                                        }
                                    }
                                    result.complete(method.invoke(instance, args));
                                    return;
                                }
                            } catch (Exception e) {
                                result.completeExceptionally(e);
                            }
                        }
                        // No method matches the parameter types
                        result.complete(Results.notFound(context));
                    }
                });
                return result;
            }
        };
    }

}
