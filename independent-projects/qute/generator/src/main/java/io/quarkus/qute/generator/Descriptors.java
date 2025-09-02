package io.quarkus.qute.generator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.qute.CompletedStage;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.EvaluatedParams;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Results;
import io.quarkus.qute.Results.NotFound;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.ValueResolvers;

public final class Descriptors {

    private Descriptors() {
    }

    public static final MethodDesc IS_ASSIGNABLE_FROM = MethodDesc.of(Class.class, "isAssignableFrom",
            boolean.class, Class.class);
    public static final MethodDesc GET_CLASS = MethodDesc.of(Object.class, "getClass", Class.class);
    public static final MethodDesc COLLECTION_SIZE = MethodDesc.of(Collection.class, "size", int.class);
    public static final MethodDesc GET_NAME = MethodDesc.of(EvalContext.class, "getName", String.class);
    public static final MethodDesc GET_BASE = MethodDesc.of(EvalContext.class, "getBase", Object.class);
    public static final MethodDesc GET_PARAMS = MethodDesc.of(EvalContext.class, "getParams", List.class);
    public static final MethodDesc GET_ATTRIBUTE = MethodDesc.of(EvalContext.class, "getAttribute",
            Object.class, String.class);
    public static final MethodDesc EVALUATE = MethodDesc.of(EvalContext.class, "evaluate",
            CompletionStage.class, Expression.class);
    static final MethodDesc LIST_GET = MethodDesc.of(List.class, "get", Object.class, int.class);
    static final MethodDesc COMPLETED_STAGE_OF = MethodDesc.of(CompletedStage.class,
            "of", CompletedStage.class, Object.class);
    public static final MethodDesc COMPLETABLE_FUTURE_ALL_OF = MethodDesc.of(CompletableFuture.class,
            "allOf",
            CompletableFuture.class, CompletableFuture[].class);
    public static final MethodDesc COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY = MethodDesc.of(
            CompletableFuture.class,
            "completeExceptionally",
            boolean.class, Throwable.class);
    public static final MethodDesc COMPLETABLE_FUTURE_COMPLETE = MethodDesc.of(CompletableFuture.class,
            "complete",
            boolean.class, Object.class);
    public static final MethodDesc COMPLETABLE_FUTURE_GET = MethodDesc.of(CompletableFuture.class,
            "get",
            Object.class);
    public static final MethodDesc CF_TO_COMPLETABLE_FUTURE = MethodDesc.of(CompletionStage.class,
            "toCompletableFuture",
            CompletableFuture.class);
    public static final MethodDesc CF_WHEN_COMPLETE = MethodDesc.of(CompletionStage.class,
            "whenComplete",
            CompletionStage.class, BiConsumer.class);
    public static final MethodDesc BOOLEAN_LOGICAL_OR = MethodDesc.of(Boolean.class, "logicalOr",
            boolean.class, boolean.class, boolean.class);
    public static final MethodDesc BOOLEAN_VALUE = MethodDesc.of(Boolean.class, "booleanValue",
            boolean.class);
    public static final MethodDesc EVALUATED_PARAMS_EVALUATE = MethodDesc.of(EvaluatedParams.class,
            "evaluate",
            EvaluatedParams.class,
            EvalContext.class);
    public static final MethodDesc EVALUATED_PARAMS_EVALUATE_MESSAGE_KEY = MethodDesc.of(
            EvaluatedParams.class,
            "evaluateMessageKey",
            EvaluatedParams.class,
            EvalContext.class);
    public static final MethodDesc EVALUATED_PARAMS_EVALUATE_MESSAGE_PARAMS = MethodDesc.of(
            EvaluatedParams.class,
            "evaluateMessageParams",
            EvaluatedParams.class,
            EvalContext.class);
    public static final MethodDesc EVALUATED_PARAMS_GET_RESULT = MethodDesc.of(EvaluatedParams.class,
            "getResult",
            Object.class,
            int.class);
    static final MethodDesc EVALUATED_PARAMS_PARAM_TYPES_MATCH = MethodDesc.of(EvaluatedParams.class,
            "parameterTypesMatch", boolean.class, boolean.class, Class[].class);
    static final MethodDesc EVALUATED_PARAMS_GET_VARARGS_RESULTS = MethodDesc.of(EvaluatedParams.class,
            "getVarargsResults", Object.class, int.class,
            Class.class);
    public static final MethodDesc PATTERN_COMPILE = MethodDesc.of(Pattern.class, "compile", Pattern.class,
            String.class);
    public static final MethodDesc PATTERN_MATCHER = MethodDesc.of(Pattern.class, "matcher", Matcher.class,
            CharSequence.class);
    public static final MethodDesc MATCHER_MATCHES = MethodDesc.of(Matcher.class, "matches", boolean.class);
    public static final ConstructorDesc OBJECT_CONSTRUCTOR = ConstructorDesc.of(Object.class);
    public static final MethodDesc RESULTS_NOT_FOUND_EC = MethodDesc.of(Results.class, "notFound",
            CompletionStage.class, EvalContext.class);
    public static final MethodDesc RESULTS_IS_NOT_FOUND = MethodDesc.of(Results.class, "isNotFound",
            boolean.class, Object.class);
    public static final MethodDesc NOT_FOUND_FROM_EC = MethodDesc.of(NotFound.class, "from",
            NotFound.class, EvalContext.class);
    public static final MethodDesc TEMPLATE_INSTANCE_DATA = MethodDesc.of(TemplateInstance.class, "data",
            TemplateInstance.class, String.class, Object.class);
    public static final MethodDesc TEMPLATE_INSTANCE_COMPUTED_DATA = MethodDesc.of(TemplateInstance.class,
            "computedData",
            TemplateInstance.class, String.class, Function.class);
    public static final MethodDesc VALUE_RESOLVERS_MATCH_CLASS = MethodDesc.of(ValueResolvers.class,
            "matchClass", boolean.class, EvalContext.class, Class.class);
    public static final MethodDesc VALUE_RESOLVERS_HAS_NO_PARAMS = MethodDesc.of(ValueResolvers.class,
            "hasNoParams", boolean.class, EvalContext.class);

    public static final FieldDesc EVALUATED_PARAMS_STAGE = FieldDesc.of(EvaluatedParams.class, "stage");

    public static final FieldDesc RESULTS_TRUE = FieldDesc.of(Results.class, "TRUE");
    public static final FieldDesc RESULTS_FALSE = FieldDesc.of(Results.class, "FALSE");
    public static final FieldDesc RESULTS_NULL = FieldDesc.of(Results.class, "NULL");
}
