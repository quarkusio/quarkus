package io.quarkus.qute.generator;

import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.EvaluatedParams;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Integers;
import io.quarkus.qute.Results;
import io.quarkus.qute.Results.Result;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

class Descriptors {

    static final MethodDescriptor IS_ASSIGNABLE_FROM = MethodDescriptor.ofMethod(Class.class, "isAssignableFrom",
            boolean.class, Class.class);
    static final MethodDescriptor GET_CLASS = MethodDescriptor.ofMethod(Object.class, "getClass", Class.class);
    static final MethodDescriptor COLLECTION_SIZE = MethodDescriptor.ofMethod(Collection.class, "size", int.class);
    static final MethodDescriptor EQUALS = MethodDescriptor.ofMethod(Object.class, "equals", boolean.class,
            Object.class);
    static final MethodDescriptor GET_NAME = MethodDescriptor.ofMethod(EvalContext.class, "getName", String.class);
    static final MethodDescriptor GET_BASE = MethodDescriptor.ofMethod(EvalContext.class, "getBase", Object.class);
    static final MethodDescriptor GET_PARAMS = MethodDescriptor.ofMethod(EvalContext.class, "getParams", List.class);
    static final MethodDescriptor EVALUATE = MethodDescriptor.ofMethod(EvalContext.class, "evaluate",
            CompletionStage.class, Expression.class);
    static final MethodDescriptor INTEGER_COMPARE = MethodDescriptor.ofMethod(Integer.class, "compare", int.class,
            int.class, int.class);
    static final MethodDescriptor LIST_GET = MethodDescriptor.ofMethod(List.class, "get", Object.class, int.class);
    static final MethodDescriptor COMPLETED_FUTURE = MethodDescriptor.ofMethod(CompletableFuture.class,
            "completedFuture", CompletableFuture.class, Object.class);
    static final MethodDescriptor COMPLETABLE_FUTURE_ALL_OF = MethodDescriptor.ofMethod(CompletableFuture.class,
            "allOf",
            CompletableFuture.class, CompletableFuture[].class);
    static final MethodDescriptor COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY = MethodDescriptor.ofMethod(
            CompletableFuture.class,
            "completeExceptionally",
            boolean.class, Throwable.class);
    static final MethodDescriptor COMPLETABLE_FUTURE_COMPLETE = MethodDescriptor.ofMethod(CompletableFuture.class,
            "complete",
            boolean.class, Object.class);
    static final MethodDescriptor COMPLETABLE_FUTURE_GET = MethodDescriptor.ofMethod(CompletableFuture.class,
            "get",
            Object.class);
    static final MethodDescriptor CF_TO_COMPLETABLE_FUTURE = MethodDescriptor.ofMethod(CompletionStage.class,
            "toCompletableFuture",
            CompletableFuture.class);
    static final MethodDescriptor CF_WHEN_COMPLETE = MethodDescriptor.ofMethod(CompletionStage.class,
            "whenComplete",
            CompletionStage.class, BiConsumer.class);
    static final MethodDescriptor BOOLEAN_LOGICAL_OR = MethodDescriptor.ofMethod(Boolean.class, "logicalOr",
            boolean.class, boolean.class, boolean.class);
    static final MethodDescriptor EVALUATED_PARAMS_EVALUATE = MethodDescriptor.ofMethod(EvaluatedParams.class, "evaluate",
            EvaluatedParams.class,
            EvalContext.class);
    static final MethodDescriptor EVALUATED_PARAMS_GET_RESULT = MethodDescriptor.ofMethod(EvaluatedParams.class, "getResult",
            Object.class,
            int.class);
    static final MethodDescriptor EVALUATED_PARAMS_PARAM_TYPES_MATCH = MethodDescriptor.ofMethod(EvaluatedParams.class,
            "parameterTypesMatch", boolean.class, boolean.class, Class[].class);
    static final MethodDescriptor EVALUATED_PARAMS_GET_VARARGS_RESULTS = MethodDescriptor.ofMethod(EvaluatedParams.class,
            "getVarargsResults", Object.class, int.class,
            Class.class);
    static final MethodDescriptor INTEGERS_IS_GT = MethodDescriptor.ofMethod(Integers.class,
            "isGt", boolean.class, int.class, int.class);

    static final FieldDescriptor RESULTS_NOT_FOUND = FieldDescriptor.of(Results.class, "NOT_FOUND",
            CompletionStage.class);
    static final FieldDescriptor RESULT_NOT_FOUND = FieldDescriptor.of(Result.class, "NOT_FOUND",
            Result.class);
    static final FieldDescriptor EVALUATED_PARAMS_STAGE = FieldDescriptor.of(EvaluatedParams.class, "stage",
            CompletionStage.class);

}
