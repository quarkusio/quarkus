package io.quarkus.qute.generator;

import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.qute.CompletedStage;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.EvaluatedParams;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Results;
import io.quarkus.qute.Results.NotFound;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Descriptors {

    private Descriptors() {
    }

    public static final MethodDescriptor IS_ASSIGNABLE_FROM = MethodDescriptor.ofMethod(Class.class, "isAssignableFrom",
            boolean.class, Class.class);
    public static final MethodDescriptor GET_CLASS = MethodDescriptor.ofMethod(Object.class, "getClass", Class.class);
    public static final MethodDescriptor COLLECTION_SIZE = MethodDescriptor.ofMethod(Collection.class, "size", int.class);
    public static final MethodDescriptor EQUALS = MethodDescriptor.ofMethod(Object.class, "equals", boolean.class,
            Object.class);
    public static final MethodDescriptor GET_NAME = MethodDescriptor.ofMethod(EvalContext.class, "getName", String.class);
    public static final MethodDescriptor GET_BASE = MethodDescriptor.ofMethod(EvalContext.class, "getBase", Object.class);
    public static final MethodDescriptor GET_PARAMS = MethodDescriptor.ofMethod(EvalContext.class, "getParams", List.class);
    public static final MethodDescriptor GET_ATTRIBUTE = MethodDescriptor.ofMethod(EvalContext.class, "getAttribute",
            Object.class, String.class);
    public static final MethodDescriptor EVALUATE = MethodDescriptor.ofMethod(EvalContext.class, "evaluate",
            CompletionStage.class, Expression.class);
    static final MethodDescriptor LIST_GET = MethodDescriptor.ofMethod(List.class, "get", Object.class, int.class);
    static final MethodDescriptor COMPLETED_STAGE = MethodDescriptor.ofMethod(CompletedStage.class,
            "of", CompletedStage.class, Object.class);
    public static final MethodDescriptor COMPLETABLE_FUTURE_ALL_OF = MethodDescriptor.ofMethod(CompletableFuture.class,
            "allOf",
            CompletableFuture.class, CompletableFuture[].class);
    public static final MethodDescriptor COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY = MethodDescriptor.ofMethod(
            CompletableFuture.class,
            "completeExceptionally",
            boolean.class, Throwable.class);
    public static final MethodDescriptor COMPLETABLE_FUTURE_COMPLETE = MethodDescriptor.ofMethod(CompletableFuture.class,
            "complete",
            boolean.class, Object.class);
    public static final MethodDescriptor COMPLETABLE_FUTURE_GET = MethodDescriptor.ofMethod(CompletableFuture.class,
            "get",
            Object.class);
    public static final MethodDescriptor CF_TO_COMPLETABLE_FUTURE = MethodDescriptor.ofMethod(CompletionStage.class,
            "toCompletableFuture",
            CompletableFuture.class);
    public static final MethodDescriptor CF_WHEN_COMPLETE = MethodDescriptor.ofMethod(CompletionStage.class,
            "whenComplete",
            CompletionStage.class, BiConsumer.class);
    public static final MethodDescriptor BOOLEAN_LOGICAL_OR = MethodDescriptor.ofMethod(Boolean.class, "logicalOr",
            boolean.class, boolean.class, boolean.class);
    public static final MethodDescriptor EVALUATED_PARAMS_EVALUATE = MethodDescriptor.ofMethod(EvaluatedParams.class,
            "evaluate",
            EvaluatedParams.class,
            EvalContext.class);
    public static final MethodDescriptor EVALUATED_PARAMS_EVALUATE_MESSAGE_KEY = MethodDescriptor.ofMethod(
            EvaluatedParams.class,
            "evaluateMessageKey",
            EvaluatedParams.class,
            EvalContext.class);
    public static final MethodDescriptor EVALUATED_PARAMS_EVALUATE_MESSAGE_PARAMS = MethodDescriptor.ofMethod(
            EvaluatedParams.class,
            "evaluateMessageParams",
            EvaluatedParams.class,
            EvalContext.class);
    public static final MethodDescriptor EVALUATED_PARAMS_GET_RESULT = MethodDescriptor.ofMethod(EvaluatedParams.class,
            "getResult",
            Object.class,
            int.class);
    static final MethodDescriptor EVALUATED_PARAMS_PARAM_TYPES_MATCH = MethodDescriptor.ofMethod(EvaluatedParams.class,
            "parameterTypesMatch", boolean.class, boolean.class, Class[].class);
    static final MethodDescriptor EVALUATED_PARAMS_GET_VARARGS_RESULTS = MethodDescriptor.ofMethod(EvaluatedParams.class,
            "getVarargsResults", Object.class, int.class,
            Class.class);
    public static final MethodDescriptor PATTERN_COMPILE = MethodDescriptor.ofMethod(Pattern.class, "compile", Pattern.class,
            String.class);
    public static final MethodDescriptor PATTERN_MATCHER = MethodDescriptor.ofMethod(Pattern.class, "matcher", Matcher.class,
            CharSequence.class);
    public static final MethodDescriptor MATCHER_MATCHES = MethodDescriptor.ofMethod(Matcher.class, "matches", boolean.class);
    public static final MethodDescriptor OBJECT_CONSTRUCTOR = MethodDescriptor.ofConstructor(Object.class);
    public static final MethodDescriptor RESULTS_NOT_FOUND_EC = MethodDescriptor.ofMethod(Results.class, "notFound",
            CompletionStage.class, EvalContext.class);
    public static final MethodDescriptor NOT_FOUND_FROM_EC = MethodDescriptor.ofMethod(NotFound.class, "from",
            NotFound.class, EvalContext.class);

    public static final FieldDescriptor EVALUATED_PARAMS_STAGE = FieldDescriptor.of(EvaluatedParams.class, "stage",
            CompletionStage.class);

}
