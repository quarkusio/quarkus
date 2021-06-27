package io.quarkus.vertx.web.deployment;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.jboss.jandex.DotName;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.vertx.web.runtime.*;
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

    static final MethodDescriptor GET_HEADERS = MethodDescriptor
            .ofMethod(HttpServerResponse.class, "headers", MultiMap.class);
    static final MethodDescriptor MULTIMAP_GET = MethodDescriptor
            .ofMethod(MultiMap.class, "get", String.class, String.class);
    static final MethodDescriptor MULTIMAP_SET = MethodDescriptor
            .ofMethod(MultiMap.class, "set", MultiMap.class, String.class, String.class);
    static final MethodDescriptor MULTIMAP_GET_ALL = MethodDescriptor
            .ofMethod(MultiMap.class, "getAll", List.class, String.class);

    static final MethodDescriptor REQUEST = MethodDescriptor
            .ofMethod(RoutingContext.class, "request", HttpServerRequest.class);
    static final MethodDescriptor REQUEST_GET_PARAM = MethodDescriptor
            .ofMethod(HttpServerRequest.class, "getParam", String.class, String.class);
    static final MethodDescriptor REQUEST_GET_HEADER = MethodDescriptor
            .ofMethod(HttpServerRequest.class, "getHeader", String.class, String.class);
    static final MethodDescriptor GET_BODY = MethodDescriptor
            .ofMethod(RoutingContext.class, "getBody", Buffer.class);
    static final MethodDescriptor GET_BODY_AS_STRING = MethodDescriptor
            .ofMethod(RoutingContext.class, "getBodyAsString", String.class);
    static final MethodDescriptor GET_BODY_AS_JSON = MethodDescriptor
            .ofMethod(RoutingContext.class, "getBodyAsJson", JsonObject.class);
    static final MethodDescriptor GET_BODY_AS_JSON_ARRAY = MethodDescriptor
            .ofMethod(RoutingContext.class, "getBodyAsJsonArray", JsonArray.class);
    static final MethodDescriptor JSON_OBJECT_MAP_TO = MethodDescriptor
            .ofMethod(JsonObject.class, "mapTo", Object.class, Class.class);
    static final MethodDescriptor REQUEST_PARAMS = MethodDescriptor
            .ofMethod(HttpServerRequest.class, "params", MultiMap.class);
    static final MethodDescriptor REQUEST_HEADERS = MethodDescriptor
            .ofMethod(HttpServerRequest.class, "headers", MultiMap.class);
    static final MethodDescriptor RESPONSE = MethodDescriptor
            .ofMethod(RoutingContext.class, "response", HttpServerResponse.class);
    static final MethodDescriptor FAIL = MethodDescriptor
            .ofMethod(RoutingContext.class, "fail", Void.TYPE, Throwable.class);
    static final MethodDescriptor FAILURE = MethodDescriptor
            .ofMethod(RoutingContext.class, "failure", Throwable.class);
    static final MethodDescriptor NEXT = MethodDescriptor
            .ofMethod(RoutingContext.class, "next", void.class);

    static final MethodDescriptor UNI_SUBSCRIBE = MethodDescriptor.ofMethod(Uni.class, "subscribe", UniSubscribe.class);
    static final MethodDescriptor UNI_SUBSCRIBE_WITH = MethodDescriptor
            .ofMethod(UniSubscribe.class, "with", Cancellable.class, Consumer.class, Consumer.class);

    static final MethodDescriptor MULTI_SUBSCRIBE_VOID = MethodDescriptor.ofMethod(MultiSupport.class, "subscribeVoid",
            Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_SUBSCRIBE_STRING = MethodDescriptor.ofMethod(MultiSupport.class, "subscribeString",
            Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_SUBSCRIBE_BUFFER = MethodDescriptor.ofMethod(MultiSupport.class, "subscribeBuffer",
            Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_SUBSCRIBE_MUTINY_BUFFER = MethodDescriptor.ofMethod(MultiSupport.class,
            "subscribeMutinyBuffer", Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_SUBSCRIBE_OBJECT = MethodDescriptor.ofMethod(MultiSupport.class, "subscribeObject",
            Void.TYPE, Multi.class, RoutingContext.class);

    static final MethodDescriptor IS_SSE = MethodDescriptor.ofMethod(MultiSseSupport.class, "isSSE", Boolean.TYPE, Multi.class);
    static final MethodDescriptor MULTI_SSE_SUBSCRIBE_STRING = MethodDescriptor.ofMethod(MultiSseSupport.class,
            "subscribeString",
            Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_SSE_SUBSCRIBE_BUFFER = MethodDescriptor.ofMethod(MultiSseSupport.class,
            "subscribeBuffer",
            Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_SSE_SUBSCRIBE_MUTINY_BUFFER = MethodDescriptor.ofMethod(MultiSseSupport.class,
            "subscribeMutinyBuffer", Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_SSE_SUBSCRIBE_OBJECT = MethodDescriptor.ofMethod(MultiSseSupport.class,
            "subscribeObject",
            Void.TYPE, Multi.class, RoutingContext.class);

    static final MethodDescriptor IS_NDJSON = MethodDescriptor.ofMethod(MultiNdjsonSupport.class, "isNdjson", Boolean.TYPE,
            Multi.class);
    static final MethodDescriptor MULTI_NDJSON_SUBSCRIBE_STRING = MethodDescriptor.ofMethod(MultiNdjsonSupport.class,
            "subscribeString",
            Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_NDJSON_SUBSCRIBE_OBJECT = MethodDescriptor.ofMethod(MultiNdjsonSupport.class,
            "subscribeObject",
            Void.TYPE, Multi.class, RoutingContext.class);

    static final MethodDescriptor IS_JSON_ARRAY = MethodDescriptor.ofMethod(MultiJsonArraySupport.class, "isJsonArray",
            Boolean.TYPE, Multi.class);
    static final MethodDescriptor MULTI_JSON_SUBSCRIBE_VOID = MethodDescriptor.ofMethod(MultiJsonArraySupport.class,
            "subscribeVoid", Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_JSON_SUBSCRIBE_STRING = MethodDescriptor.ofMethod(MultiJsonArraySupport.class,
            "subscribeString", Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_JSON_SUBSCRIBE_BUFFER = MethodDescriptor.ofMethod(MultiJsonArraySupport.class,
            "subscribeBuffer", Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_JSON_SUBSCRIBE_MUTINY_BUFFER = MethodDescriptor.ofMethod(MultiJsonArraySupport.class,
            "subscribeMutinyBuffer", Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_JSON_SUBSCRIBE_OBJECT = MethodDescriptor.ofMethod(MultiJsonArraySupport.class,
            "subscribeObject", Void.TYPE, Multi.class, RoutingContext.class);
    static final MethodDescriptor MULTI_JSON_FAIL = MethodDescriptor.ofMethod(MultiJsonArraySupport.class,
            "fail", Void.TYPE, RoutingContext.class);

    static final MethodDescriptor END = MethodDescriptor.ofMethod(HttpServerResponse.class, "end", Future.class);
    static final MethodDescriptor END_WITH_STRING = MethodDescriptor
            .ofMethod(HttpServerResponse.class, "end", Future.class, String.class);
    static final MethodDescriptor END_WITH_BUFFER = MethodDescriptor
            .ofMethod(HttpServerResponse.class, "end", Future.class, Buffer.class);
    static final MethodDescriptor SET_STATUS = MethodDescriptor
            .ofMethod(HttpServerResponse.class, "setStatusCode", HttpServerResponse.class, Integer.TYPE);
    static final MethodDescriptor MUTINY_GET_DELEGATE = MethodDescriptor
            .ofMethod(io.vertx.mutiny.core.buffer.Buffer.class, "getDelegate", Buffer.class);
    static final MethodDescriptor JSON_ENCODE = MethodDescriptor
            .ofMethod(Json.class, "encode", String.class, Object.class);
    static final MethodDescriptor ARC_CONTAINER = MethodDescriptor
            .ofMethod(Arc.class, "container", ArcContainer.class);
    static final MethodDescriptor ARC_CONTAINER_GET_ACTIVE_CONTEXT = MethodDescriptor
            .ofMethod(ArcContainer.class,
                    "getActiveContext", InjectableContext.class, Class.class);
    static final MethodDescriptor ARC_CONTAINER_BEAN = MethodDescriptor.ofMethod(ArcContainer.class, "bean",
            InjectableBean.class, String.class);
    static final MethodDescriptor BEAN_GET_SCOPE = MethodDescriptor.ofMethod(InjectableBean.class, "getScope",
            Class.class);
    static final MethodDescriptor CONTEXT_GET = MethodDescriptor.ofMethod(Context.class, "get", Object.class,
            Contextual.class,
            CreationalContext.class);
    static final MethodDescriptor CONTEXT_GET_IF_PRESENT = MethodDescriptor
            .ofMethod(Context.class, "get", Object.class,
                    Contextual.class);
    static final MethodDescriptor INJECTABLE_REF_PROVIDER_GET = MethodDescriptor.ofMethod(
            InjectableReferenceProvider.class,
            "get", Object.class,
            CreationalContext.class);
    static final MethodDescriptor INJECTABLE_BEAN_DESTROY = MethodDescriptor
            .ofMethod(InjectableBean.class, "destroy",
                    void.class, Object.class,
                    CreationalContext.class);
    static final MethodDescriptor ROUTE_HANDLER_CONSTRUCTOR = MethodDescriptor.ofConstructor(RouteHandler.class);

    static final MethodDescriptor ROUTE_HANDLERS_SET_CONTENT_TYPE = MethodDescriptor
            .ofMethod(RouteHandlers.class, "setContentType", void.class, RoutingContext.class, String.class);

    static final MethodDescriptor OPTIONAL_OF_NULLABLE = MethodDescriptor
            .ofMethod(Optional.class, "ofNullable", Optional.class, Object.class);

    static final String VALIDATION_VALIDATOR = "javax.validation.Validator";
    static final String VALIDATION_CONSTRAINT_VIOLATION_EXCEPTION = "javax.validation.ConstraintViolationException";

    static final MethodDescriptor VALIDATION_GET_VALIDATOR = MethodDescriptor.ofMethod(ValidationSupport.class, "getValidator",
            "javax.validation.Validator", ArcContainer.class);
    static final MethodDescriptor VALIDATION_MAP_VIOLATIONS_TO_JSON = MethodDescriptor
            .ofMethod(ValidationSupport.class, "mapViolationsToJson", String.class, Set.class,
                    HttpServerResponse.class);
    static final MethodDescriptor VALIDATION_HANDLE_VIOLATION_EXCEPTION = MethodDescriptor
            .ofMethod(ValidationSupport.class.getName(), "handleViolationException",
                    Void.TYPE.getName(), Methods.VALIDATION_CONSTRAINT_VIOLATION_EXCEPTION,
                    RoutingContext.class.getName(), Boolean.TYPE.getName());

    static final MethodDescriptor VALIDATOR_VALIDATE = MethodDescriptor
            .ofMethod("javax.validation.Validator", "validate", "java.util.Set",
                    Object.class, Class[].class);
    static final MethodDescriptor SET_IS_EMPTY = MethodDescriptor.ofMethod(Set.class, "isEmpty", Boolean.TYPE);

    static final MethodDescriptor IS_ASSIGNABLE_FROM = MethodDescriptor.ofMethod(Class.class, "isAssignableFrom",
            boolean.class, Class.class);
    static final MethodDescriptor GET_CLASS = MethodDescriptor.ofMethod(Object.class, "getClass", Class.class);

    static final MethodDescriptor STRING_CHAR_AT = MethodDescriptor.ofMethod(String.class, "charAt", int.class);
    static final MethodDescriptor INTEGER_VALUE_OF = MethodDescriptor.ofMethod(Integer.class, "valueOf", Integer.class,
            String.class);
    static final MethodDescriptor LONG_VALUE_OF = MethodDescriptor.ofMethod(Long.class, "valueOf", Long.class, String.class);
    static final MethodDescriptor BOOLEAN_VALUE_OF = MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class,
            String.class);
    static final MethodDescriptor CHARACTER_VALUE_OF = MethodDescriptor.ofMethod(Character.class, "valueOf", Character.class,
            char.class);
    static final MethodDescriptor FLOAT_VALUE_OF = MethodDescriptor.ofMethod(Float.class, "valueOf", Float.class, String.class);
    static final MethodDescriptor DOUBLE_VALUE_OF = MethodDescriptor.ofMethod(Double.class, "valueOf", Double.class,
            String.class);
    static final MethodDescriptor SHORT_VALUE_OF = MethodDescriptor.ofMethod(Short.class, "valueOf", Short.class, String.class);
    static final MethodDescriptor BYTE_VALUE_OF = MethodDescriptor.ofMethod(Byte.class, "valueOf", Byte.class, String.class);

    static final MethodDescriptor COLLECTION_SIZE = MethodDescriptor.ofMethod(Collection.class, "size", int.class);
    static final MethodDescriptor COLLECTION_ITERATOR = MethodDescriptor.ofMethod(Collection.class, "iterator", Iterator.class);
    static final MethodDescriptor COLLECTION_ADD = MethodDescriptor.ofMethod(Collection.class, "add", boolean.class,
            Object.class);
    static final MethodDescriptor ITERATOR_NEXT = MethodDescriptor.ofMethod(Iterator.class, "next", Object.class);
    static final MethodDescriptor ITERATOR_HAS_NEXT = MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class);

    public static final MethodDescriptor CS_WHEN_COMPLETE = MethodDescriptor.ofMethod(CompletionStage.class,
            "whenComplete",
            CompletionStage.class, BiConsumer.class);

    private Methods() {
        // Avoid direct instantiation
    }

    static void returnAndClose(BytecodeCreator creator) {
        creator.returnValue(null);
        creator.close();
    }

    static boolean isNoContent(HandlerDescriptor descriptor) {
        return descriptor.getContentType().name()
                .equals(DotName.createSimple(Void.class.getName()));
    }

    static ResultHandle createNpeBecauseItemIfNull(BytecodeCreator writer) {
        return writer.newInstance(
                MethodDescriptor.ofConstructor(NullPointerException.class, String.class),
                writer.load("Invalid value returned by Uni: `null`"));
    }

    static MethodDescriptor getEndMethodForContentType(HandlerDescriptor descriptor) {
        if (descriptor.isContentTypeBuffer() || descriptor.isContentTypeMutinyBuffer()) {
            return END_WITH_BUFFER;
        }
        return END_WITH_STRING;
    }

    static void setContentTypeToJson(ResultHandle response, BytecodeCreator invoke) {
        ResultHandle ct = invoke.load("Content-Type");
        ResultHandle headers = invoke.invokeInterfaceMethod(GET_HEADERS, response);
        ResultHandle current = invoke.invokeInterfaceMethod(MULTIMAP_GET, headers, ct);
        BytecodeCreator branch = invoke.ifNull(current).trueBranch();
        branch.invokeInterfaceMethod(MULTIMAP_SET, headers, ct, branch.load("application/json"));
        branch.close();
    }

    /**
     * Generate the following code:
     *
     * <pre>
     * String s = null;
     * Set<ConstraintViolation<Object>> violations = validator.validate(res);
     * if (!violations.isEmpty()) {
     *    s = ValidationSupport.mapViolationsToJson(violations, response);
     * } else {
     *    s = res.encode()
     * }
     * </pre>
     */
    public static ResultHandle validateProducedItem(ResultHandle response, BytecodeCreator writer, ResultHandle res,
            FieldCreator validatorField, ResultHandle owner) {

        AssignableResultHandle result = writer.createVariable(String.class);
        writer.assign(result, writer.loadNull());

        ResultHandle validator = writer.readInstanceField(validatorField.getFieldDescriptor(), owner);
        ResultHandle violations = writer.invokeInterfaceMethod(
                VALIDATOR_VALIDATE, validator, res, writer.newArray(Class.class, 0));

        ResultHandle isEmpty = writer.invokeInterfaceMethod(SET_IS_EMPTY, violations);
        BranchResult ifNoViolations = writer.ifTrue(isEmpty);

        ResultHandle encoded = ifNoViolations.trueBranch().invokeStaticMethod(JSON_ENCODE, res);
        ifNoViolations.trueBranch().assign(result, encoded);
        ifNoViolations.trueBranch().close();

        ResultHandle json = ifNoViolations.falseBranch().invokeStaticMethod(VALIDATION_MAP_VIOLATIONS_TO_JSON, violations,
                response);
        ifNoViolations.falseBranch().assign(result, json);
        ifNoViolations.falseBranch().close();

        return result;

    }
}
