package io.quarkus.vertx.web.deployment;

import static io.quarkus.gizmo2.Reflection2Gizmo.classDescOf;
import static java.lang.constant.ConstantDescs.CD_boolean;
import static java.lang.constant.ConstantDescs.CD_void;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.jandex.DotName;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.FieldVar;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.vertx.web.runtime.MultiJsonArraySupport;
import io.quarkus.vertx.web.runtime.MultiNdjsonSupport;
import io.quarkus.vertx.web.runtime.MultiSseSupport;
import io.quarkus.vertx.web.runtime.MultiSupport;
import io.quarkus.vertx.web.runtime.RouteHandler;
import io.quarkus.vertx.web.runtime.RouteHandlers;
import io.quarkus.vertx.web.runtime.ValidationSupport;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniSubscribe;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

class Methods {

    static final MethodDesc GET_HEADERS = MethodDesc.of(HttpServerResponse.class, "headers", MultiMap.class);
    static final MethodDesc MULTIMAP_GET = MethodDesc.of(MultiMap.class, "get", String.class, String.class);
    static final MethodDesc MULTIMAP_SET = MethodDesc.of(MultiMap.class, "set", MultiMap.class, String.class, String.class);
    static final MethodDesc MULTIMAP_GET_ALL = MethodDesc.of(MultiMap.class, "getAll", List.class, String.class);

    static final MethodDesc REQUEST = MethodDesc.of(RoutingContext.class, "request", HttpServerRequest.class);
    static final MethodDesc REQUEST_GET_PARAM = MethodDesc.of(HttpServerRequest.class, "getParam", String.class, String.class);
    static final MethodDesc REQUEST_GET_HEADER = MethodDesc.of(HttpServerRequest.class, "getHeader",
            String.class, String.class);
    static final MethodDesc GET_BODY = MethodDesc.of(RoutingContext.class, "getBody", Buffer.class);
    static final MethodDesc GET_BODY_AS_STRING = MethodDesc.of(RoutingContext.class, "getBodyAsString", String.class);
    static final MethodDesc GET_BODY_AS_JSON = MethodDesc.of(RoutingContext.class, "getBodyAsJson", JsonObject.class);
    static final MethodDesc GET_BODY_AS_JSON_ARRAY = MethodDesc.of(RoutingContext.class, "getBodyAsJsonArray", JsonArray.class);
    static final MethodDesc JSON_OBJECT_MAP_TO = MethodDesc.of(JsonObject.class, "mapTo", Object.class, Class.class);
    static final MethodDesc REQUEST_PARAMS = MethodDesc.of(HttpServerRequest.class, "params", MultiMap.class);
    static final MethodDesc REQUEST_HEADERS = MethodDesc.of(HttpServerRequest.class, "headers", MultiMap.class);
    static final MethodDesc RESPONSE = MethodDesc.of(RoutingContext.class, "response", HttpServerResponse.class);
    static final MethodDesc FAIL = MethodDesc.of(RoutingContext.class, "fail", void.class, Throwable.class);
    static final MethodDesc FAILURE = MethodDesc.of(RoutingContext.class, "failure", Throwable.class);
    static final MethodDesc NEXT = MethodDesc.of(RoutingContext.class, "next", void.class);

    static final MethodDesc UNI_SUBSCRIBE = MethodDesc.of(Uni.class, "subscribe", UniSubscribe.class);
    static final MethodDesc UNI_SUBSCRIBE_WITH = MethodDesc.of(UniSubscribe.class, "with",
            Cancellable.class, Consumer.class, Consumer.class);

    static final MethodDesc MULTI_SUBSCRIBE_VOID = MethodDesc.of(MultiSupport.class, "subscribeVoid",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_SUBSCRIBE_STRING = MethodDesc.of(MultiSupport.class, "subscribeString",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_SUBSCRIBE_BUFFER = MethodDesc.of(MultiSupport.class, "subscribeBuffer",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_SUBSCRIBE_MUTINY_BUFFER = MethodDesc.of(MultiSupport.class, "subscribeMutinyBuffer",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_SUBSCRIBE_OBJECT = MethodDesc.of(MultiSupport.class, "subscribeObject",
            void.class, Multi.class, RoutingContext.class);

    static final MethodDesc IS_SSE = MethodDesc.of(MultiSseSupport.class, "isSSE", boolean.class, Multi.class);
    static final MethodDesc MULTI_SSE_SUBSCRIBE_STRING = MethodDesc.of(MultiSseSupport.class, "subscribeString",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_SSE_SUBSCRIBE_BUFFER = MethodDesc.of(MultiSseSupport.class, "subscribeBuffer",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_SSE_SUBSCRIBE_MUTINY_BUFFER = MethodDesc.of(MultiSseSupport.class, "subscribeMutinyBuffer",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_SSE_SUBSCRIBE_OBJECT = MethodDesc.of(MultiSseSupport.class, "subscribeObject",
            void.class, Multi.class, RoutingContext.class);

    static final MethodDesc IS_NDJSON = MethodDesc.of(MultiNdjsonSupport.class, "isNdjson",
            boolean.class, Multi.class);
    static final MethodDesc MULTI_NDJSON_SUBSCRIBE_STRING = MethodDesc.of(MultiNdjsonSupport.class, "subscribeString",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_NDJSON_SUBSCRIBE_OBJECT = MethodDesc.of(MultiNdjsonSupport.class, "subscribeObject",
            void.class, Multi.class, RoutingContext.class);

    static final MethodDesc IS_JSON_ARRAY = MethodDesc.of(MultiJsonArraySupport.class, "isJsonArray",
            boolean.class, Multi.class);
    static final MethodDesc MULTI_JSON_SUBSCRIBE_VOID = MethodDesc.of(MultiJsonArraySupport.class, "subscribeVoid",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_JSON_SUBSCRIBE_STRING = MethodDesc.of(MultiJsonArraySupport.class, "subscribeString",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_JSON_SUBSCRIBE_OBJECT = MethodDesc.of(MultiJsonArraySupport.class, "subscribeObject",
            void.class, Multi.class, RoutingContext.class);
    static final MethodDesc MULTI_JSON_FAIL = MethodDesc.of(MultiJsonArraySupport.class, "fail",
            void.class, RoutingContext.class);

    static final MethodDesc END = MethodDesc.of(HttpServerResponse.class, "end", Future.class);
    static final MethodDesc END_WITH_STRING = MethodDesc.of(HttpServerResponse.class, "end", Future.class, String.class);
    static final MethodDesc END_WITH_BUFFER = MethodDesc.of(HttpServerResponse.class, "end", Future.class, Buffer.class);
    static final MethodDesc SET_STATUS = MethodDesc.of(HttpServerResponse.class, "setStatusCode",
            HttpServerResponse.class, Integer.TYPE);
    static final MethodDesc MUTINY_GET_DELEGATE = MethodDesc.of(io.vertx.mutiny.core.buffer.Buffer.class, "getDelegate",
            Buffer.class);

    static final MethodDesc JSON_ENCODE = MethodDesc.of(Json.class, "encode", String.class, Object.class);

    static final MethodDesc ARC_CONTAINER = MethodDesc.of(Arc.class, "container", ArcContainer.class);
    static final MethodDesc ARC_CONTAINER_GET_ACTIVE_CONTEXT = MethodDesc.of(ArcContainer.class, "getActiveContext",
            InjectableContext.class, Class.class);
    static final MethodDesc ARC_CONTAINER_BEAN = MethodDesc.of(ArcContainer.class, "bean",
            InjectableBean.class, String.class);
    static final MethodDesc BEAN_GET_SCOPE = MethodDesc.of(InjectableBean.class, "getScope", Class.class);
    static final MethodDesc CONTEXT_GET = MethodDesc.of(Context.class, "get",
            Object.class, Contextual.class, CreationalContext.class);
    static final MethodDesc CONTEXT_GET_IF_PRESENT = MethodDesc.of(Context.class, "get",
            Object.class, Contextual.class);
    static final MethodDesc INJECTABLE_REF_PROVIDER_GET = MethodDesc.of(InjectableReferenceProvider.class, "get",
            Object.class, CreationalContext.class);
    static final MethodDesc INJECTABLE_BEAN_DESTROY = MethodDesc.of(InjectableBean.class, "destroy",
            void.class, Object.class, CreationalContext.class);
    static final ConstructorDesc CREATIONAL_CONTEXT_IMPL_CTOR = ConstructorDesc.of(CreationalContextImpl.class,
            Contextual.class);

    static final ConstructorDesc ROUTE_HANDLER_CTOR = ConstructorDesc.of(RouteHandler.class);

    static final MethodDesc ROUTE_HANDLERS_SET_CONTENT_TYPE = MethodDesc.of(RouteHandlers.class, "setContentType",
            void.class, RoutingContext.class, String.class);

    static final MethodDesc OPTIONAL_OF_NULLABLE = MethodDesc.of(Optional.class, "ofNullable", Optional.class, Object.class);

    private static final String VALIDATOR = "jakarta.validation.Validator";
    private static final String CONSTRAINT_VIOLATION_EXCEPTION = "jakarta.validation.ConstraintViolationException";
    static final ClassDesc VALIDATION_VALIDATOR = ClassDesc.of(VALIDATOR);
    static final ClassDesc VALIDATION_CONSTRAINT_VIOLATION_EXCEPTION = ClassDesc.of(CONSTRAINT_VIOLATION_EXCEPTION);

    static final MethodDesc VALIDATOR_VALIDATE = InterfaceMethodDesc.of(VALIDATION_VALIDATOR, "validate",
            Set.class, Object.class, Class[].class);

    static final MethodDesc VALIDATION_GET_VALIDATOR = MethodDesc.of(ValidationSupport.class, "getValidator",
            MethodTypeDesc.of(VALIDATION_VALIDATOR, classDescOf(ArcContainer.class)));
    static final MethodDesc VALIDATION_MAP_VIOLATIONS_TO_JSON = MethodDesc.of(ValidationSupport.class, "mapViolationsToJson",
            String.class, Set.class, HttpServerResponse.class);
    static final MethodDesc VALIDATION_HANDLE_VIOLATION = MethodDesc.of(ValidationSupport.class, "handleViolationException",
            MethodTypeDesc.of(CD_void, VALIDATION_CONSTRAINT_VIOLATION_EXCEPTION, classDescOf(RoutingContext.class),
                    CD_boolean));

    static final MethodDesc IS_ASSIGNABLE_FROM = MethodDesc.of(Class.class, "isAssignableFrom", boolean.class, Class.class);

    static final MethodDesc INTEGER_VALUE_OF = MethodDesc.of(Integer.class, "valueOf", Integer.class, String.class);
    static final MethodDesc LONG_VALUE_OF = MethodDesc.of(Long.class, "valueOf", Long.class, String.class);
    static final MethodDesc BOOLEAN_VALUE_OF = MethodDesc.of(Boolean.class, "valueOf", Boolean.class, String.class);
    static final MethodDesc CHARACTER_VALUE_OF = MethodDesc.of(Character.class, "valueOf", Character.class, char.class);
    static final MethodDesc FLOAT_VALUE_OF = MethodDesc.of(Float.class, "valueOf", Float.class, String.class);
    static final MethodDesc DOUBLE_VALUE_OF = MethodDesc.of(Double.class, "valueOf", Double.class, String.class);
    static final MethodDesc SHORT_VALUE_OF = MethodDesc.of(Short.class, "valueOf", Short.class, String.class);
    static final MethodDesc BYTE_VALUE_OF = MethodDesc.of(Byte.class, "valueOf", Byte.class, String.class);

    public static final MethodDesc CS_WHEN_COMPLETE = MethodDesc.of(CompletionStage.class, "whenComplete",
            CompletionStage.class, BiConsumer.class);

    private Methods() {
        // Avoid direct instantiation
    }

    static boolean isNoContent(HandlerDescriptor descriptor) {
        return descriptor.getPayloadType().name().equals(DotName.createSimple(Void.class));
    }

    static Expr createNpeItemIsNull(BlockCreator bc) {
        return bc.new_(ConstructorDesc.of(NullPointerException.class, String.class),
                Const.of("Invalid value returned by Uni: `null`"));
    }

    static MethodDesc getEndMethodForContentType(HandlerDescriptor descriptor) {
        if (descriptor.isPayloadBuffer() || descriptor.isPayloadMutinyBuffer()) {
            return END_WITH_BUFFER;
        }
        return END_WITH_STRING;
    }

    static void setContentTypeToJson(Var response, BlockCreator b0) {
        Const contentType = Const.of("Content-Type");
        LocalVar headers = b0.localVar("headers", b0.invokeInterface(GET_HEADERS, response));
        Expr current = b0.invokeInterface(MULTIMAP_GET, headers, contentType);
        b0.ifNull(current, b1 -> {
            b1.invokeInterface(MULTIMAP_SET, headers, contentType, Const.of("application/json"));
        });
    }

    /**
     * Generate the following code:
     *
     * <pre>
     * String result = null;
     * Set&lt;ConstraintViolation&lt;Object>> violations = validator.validate(res);
     * if (violations.isEmpty()) {
     *    result = res.encode()
     * } else {
     *    result = ValidationSupport.mapViolationsToJson(violations, response);
     * }
     * </pre>
     *
     * Note that {@code this_} is always either {@code This} or a captured {@code Var}.
     *
     */
    public static Var validateProducedItem(Var response, BlockCreator b0, Var res,
            Expr this_, FieldDesc validatorField) {

        FieldVar validator = this_.field(validatorField);
        LocalVar violations = b0.localVar("violations",
                b0.invokeInterface(VALIDATOR_VALIDATE, validator, res, b0.newArray(Class.class)));

        LocalVar result = b0.localVar("result", Const.ofDefault(String.class));

        b0.ifElse(b0.withSet(violations).isEmpty(), b1 -> {
            Expr json = b1.invokeStatic(JSON_ENCODE, res);
            b1.set(result, json);
        }, b1 -> {
            Expr json = b1.invokeStatic(VALIDATION_MAP_VIOLATIONS_TO_JSON, violations, response);
            b1.set(result, json);
        });

        return result;
    }
}
