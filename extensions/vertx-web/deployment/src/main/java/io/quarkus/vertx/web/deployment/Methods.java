package io.quarkus.vertx.web.deployment;

import java.util.function.Consumer;

import org.jboss.jandex.DotName;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniSubscribe;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class Methods {

    static final MethodDescriptor GET_HEADERS = MethodDescriptor
            .ofMethod(HttpServerResponse.class, "headers", MultiMap.class);
    static final MethodDescriptor MULTIMAP_GET = MethodDescriptor
            .ofMethod(MultiMap.class, "get", String.class, String.class);
    static final MethodDescriptor MULTIMAP_SET = MethodDescriptor
            .ofMethod(MultiMap.class, "set", MultiMap.class, String.class, String.class);

    static final MethodDescriptor RESPONSE = MethodDescriptor
            .ofMethod(RoutingContext.class, "response", HttpServerResponse.class);

    static final MethodDescriptor FAIL = MethodDescriptor
            .ofMethod(RoutingContext.class, "fail", Void.TYPE, Throwable.class);
    static final MethodDescriptor SUBSCRIBE = MethodDescriptor.ofMethod(Uni.class, "subscribe", UniSubscribe.class);

    static final MethodDescriptor SUBSCRIBE_WITH = MethodDescriptor
            .ofMethod(UniSubscribe.class, "with", Cancellable.class, Consumer.class, Consumer.class);
    static final MethodDescriptor END = MethodDescriptor.ofMethod(HttpServerResponse.class, "end", Void.TYPE);

    static final MethodDescriptor END_WITH_STRING = MethodDescriptor
            .ofMethod(HttpServerResponse.class, "end", Void.TYPE, String.class);
    static final MethodDescriptor END_WITH_BUFFER = MethodDescriptor
            .ofMethod(HttpServerResponse.class, "end", Void.TYPE, Buffer.class);
    static final MethodDescriptor SET_STATUS = MethodDescriptor
            .ofMethod(HttpServerResponse.class, "setStatusCode", HttpServerResponse.class, Integer.TYPE);
    static final MethodDescriptor RX_GET_DELEGATE = MethodDescriptor
            .ofMethod(io.vertx.reactivex.core.buffer.Buffer.class, "getDelegate", Buffer.class);

    static final MethodDescriptor MUTINY_GET_DELEGATE = MethodDescriptor
            .ofMethod(io.vertx.mutiny.core.buffer.Buffer.class, "getDelegate", Buffer.class);
    static final MethodDescriptor JSON_ENCODE = MethodDescriptor
            .ofMethod(Json.class, "encode", String.class, Object.class);

    private Methods() {
        // Avoid direct instantiation
    }

    static void fail(BytecodeCreator creator, ResultHandle rc, ResultHandle exception) {
        creator.invokeInterfaceMethod(FAIL, rc, exception);
    }

    public static void returnAndClose(BytecodeCreator creator) {
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
        if (descriptor.isContentTypeBuffer() || descriptor.isContentTypeRxBuffer() || descriptor
                .isContentTypeMutinyBuffer()) {
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
}
