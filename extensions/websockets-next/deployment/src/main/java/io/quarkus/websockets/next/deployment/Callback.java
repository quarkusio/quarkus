package io.quarkus.websockets.next.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.KotlinDotNames;
import io.quarkus.arc.processor.KotlinUtils;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.websockets.next.WebSocketException;
import io.quarkus.websockets.next.deployment.CallbackArgument.InvocationBytecodeContext;
import io.quarkus.websockets.next.deployment.CallbackArgument.ParameterContext;
import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint.ExecutionModel;
import io.quarkus.websockets.next.runtime.WebSocketEndpointBase;

/**
 * Represents either an endpoint callback or a global error handler.
 */
public class Callback {

    public final Target target;
    public final String endpointPath;
    public final AnnotationInstance annotation;
    public final BeanInfo bean;
    public final MethodInfo method;
    public final ExecutionModel executionModel;
    public final MessageType messageType;
    public final List<CallbackArgument> arguments;

    public Callback(Target target, AnnotationInstance annotation, BeanInfo bean, MethodInfo method,
            ExecutionModel executionModel, CallbackArgumentsBuildItem callbackArguments,
            TransformedAnnotationsBuildItem transformedAnnotations, String endpointPath, IndexView index) {
        this.target = target;
        this.bean = bean;
        this.method = method;
        this.annotation = annotation;
        this.executionModel = executionModel;
        if (WebSocketDotNames.ON_BINARY_MESSAGE.equals(annotation.name())) {
            this.messageType = MessageType.BINARY;
        } else if (WebSocketDotNames.ON_TEXT_MESSAGE.equals(annotation.name())) {
            this.messageType = MessageType.TEXT;
        } else if (WebSocketDotNames.ON_PING_MESSAGE.equals(annotation.name())) {
            this.messageType = MessageType.PING;
        } else if (WebSocketDotNames.ON_PONG_MESSAGE.equals(annotation.name())) {
            this.messageType = MessageType.PONG;
        } else {
            this.messageType = MessageType.NONE;
        }
        this.endpointPath = endpointPath;
        this.arguments = collectArguments(annotation, method, callbackArguments, transformedAnnotations, index);
    }

    public boolean isGlobal() {
        return endpointPath == null;
    }

    public boolean isClient() {
        return target == Target.CLIENT;
    }

    public boolean isServer() {
        return target == Target.SERVER;
    }

    public boolean isOnOpen() {
        return annotation.name().equals(WebSocketDotNames.ON_OPEN);
    }

    public boolean isOnClose() {
        return annotation.name().equals(WebSocketDotNames.ON_CLOSE);
    }

    public boolean isOnError() {
        return annotation.name().equals(WebSocketDotNames.ON_ERROR);
    }

    public Type returnType() {
        return method.returnType();
    }

    public Type messageParamType() {
        return acceptsMessage() ? method.parameterType(0) : null;
    }

    public boolean isReturnTypeVoid() {
        return returnType().kind() == Kind.VOID;
    }

    public boolean isReturnTypeUni() {
        return WebSocketDotNames.UNI.equals(returnType().name());
    }

    public boolean isReturnTypeMulti() {
        return WebSocketDotNames.MULTI.equals(returnType().name());
    }

    public boolean isKotlinSuspendFunction() {
        return KotlinUtils.isKotlinSuspendMethod(method);
    }

    public boolean isKotlinSuspendFunctionReturningUnit() {
        return KotlinUtils.isKotlinSuspendMethod(method)
                && KotlinUtils.getKotlinSuspendMethodResult(method).name().equals(KotlinDotNames.UNIT);
    }

    public boolean acceptsMessage() {
        return messageType != MessageType.NONE;
    }

    public boolean acceptsBinaryMessage() {
        return messageType == MessageType.BINARY || messageType == MessageType.PING || messageType == MessageType.PONG;
    }

    public boolean acceptsMulti() {
        return acceptsMessage() && method.parameterType(0).name().equals(WebSocketDotNames.MULTI);
    }

    public Callback.MessageType messageType() {
        return messageType;
    }

    public boolean broadcast() {
        AnnotationValue broadcastValue = annotation.value("broadcast");
        return broadcastValue != null && broadcastValue.asBoolean();
    }

    public DotName getInputCodec() {
        return getCodec("codec");
    }

    public DotName getOutputCodec() {
        DotName output = getCodec("outputCodec");
        return output != null ? output : getInputCodec();
    }

    public String asString() {
        return method.declaringClass().name() + "#" + method.name() + "()";
    }

    private DotName getCodec(String valueName) {
        AnnotationValue codecValue = annotation.value(valueName);
        if (codecValue != null) {
            return codecValue.asClass().name();
        }
        return null;
    }

    public enum MessageType {
        NONE,
        PING,
        PONG,
        TEXT,
        BINARY
    }

    public enum Target {
        CLIENT,
        SERVER,
        UNDEFINED
    }

    public ResultHandle[] generateArguments(ResultHandle endpointThis, BytecodeCreator bytecode,
            TransformedAnnotationsBuildItem transformedAnnotations, IndexView index) {
        if (arguments.isEmpty()) {
            return new ResultHandle[] {};
        }
        ResultHandle[] resultHandles = new ResultHandle[arguments.size()];
        int idx = 0;
        for (CallbackArgument argument : arguments) {
            resultHandles[idx] = argument.get(
                    invocationBytecodeContext(annotation, method.parameters().get(idx), transformedAnnotations, index,
                            endpointThis, bytecode));
            idx++;
        }
        return resultHandles;
    }

    private List<CallbackArgument> collectArguments(AnnotationInstance annotation, MethodInfo method,
            CallbackArgumentsBuildItem callbackArguments, TransformedAnnotationsBuildItem transformedAnnotations,
            IndexView index) {
        List<MethodParameterInfo> parameters = method.parameters();
        if (parameters.isEmpty()) {
            return List.of();
        }
        List<CallbackArgument> arguments = new ArrayList<>(parameters.size());
        for (MethodParameterInfo parameter : parameters) {
            List<CallbackArgument> found = callbackArguments
                    .findMatching(parameterContext(annotation, parameter, transformedAnnotations, index));
            if (found.isEmpty()) {
                String msg = String.format("Unable to inject @%s callback parameter '%s' declared on %s: no injector found",
                        DotNames.simpleName(annotation.name()),
                        parameter.name() != null ? parameter.name() : "#" + parameter.position(),
                        asString());
                throw new WebSocketException(msg);
            } else if (found.size() > 1 && (found.get(0).priotity() == found.get(1).priotity())) {
                String msg = String.format(
                        "Unable to inject @%s callback parameter '%s' declared on %s: ambiguous injectors found: %s",
                        DotNames.simpleName(annotation.name()),
                        parameter.name() != null ? parameter.name() : "#" + parameter.position(),
                        asString(),
                        found.stream().map(p -> p.getClass().getSimpleName() + ":" + p.priotity()));
                throw new WebSocketException(msg);
            }
            arguments.add(found.get(0));
        }
        return List.copyOf(arguments);
    }

    Type argumentType(Predicate<CallbackArgument> filter) {
        for (int i = 0; i < arguments.size(); i++) {
            if (filter.test(arguments.get(i))) {
                return method.parameterType(i);
            }
        }
        return null;
    }

    private ParameterContext parameterContext(AnnotationInstance callbackAnnotation, MethodParameterInfo parameter,
            TransformedAnnotationsBuildItem transformedAnnotations, IndexView index) {
        return new ParameterContext() {

            @Override
            public Target callbackTarget() {
                return target;
            }

            @Override
            public MethodParameterInfo parameter() {
                return parameter;
            }

            @Override
            public Set<AnnotationInstance> parameterAnnotations() {
                return Annotations.getParameterAnnotations(
                        transformedAnnotations::getAnnotations, parameter.method(), parameter.position());
            }

            @Override
            public AnnotationInstance callbackAnnotation() {
                return callbackAnnotation;
            }

            @Override
            public String endpointPath() {
                return endpointPath;
            }

            @Override
            public IndexView index() {
                return index;
            }

        };
    }

    private InvocationBytecodeContext invocationBytecodeContext(AnnotationInstance callbackAnnotation,
            MethodParameterInfo parameter, TransformedAnnotationsBuildItem transformedAnnotations, IndexView index,
            ResultHandle endpointThis, BytecodeCreator bytecode) {
        return new InvocationBytecodeContext() {

            @Override
            public Target callbackTarget() {
                return target;
            }

            @Override
            public AnnotationInstance callbackAnnotation() {
                return callbackAnnotation;
            }

            @Override
            public MethodParameterInfo parameter() {
                return parameter;
            }

            @Override
            public Set<AnnotationInstance> parameterAnnotations() {
                return Annotations.getParameterAnnotations(
                        transformedAnnotations::getAnnotations, parameter.method(), parameter.position());
            }

            @Override
            public String endpointPath() {
                return endpointPath;
            }

            @Override
            public IndexView index() {
                return index;
            }

            @Override
            public BytecodeCreator bytecode() {
                return bytecode;
            }

            @Override
            public ResultHandle getPayload() {
                return acceptsMessage() || callbackAnnotation.name().equals(WebSocketDotNames.ON_ERROR)
                        ? bytecode.getMethodParam(0)
                        : null;
            }

            @Override
            public ResultHandle getDecodedMessage(Type parameterType) {
                return acceptsMessage()
                        ? WebSocketProcessor.decodeMessage(endpointThis, bytecode, acceptsBinaryMessage(),
                                parameterType,
                                getPayload(), Callback.this)
                        : null;
            }

            @Override
            public ResultHandle getConnection() {
                return bytecode.readInstanceField(
                        FieldDescriptor.of(WebSocketEndpointBase.class, "connection", WebSocketConnectionBase.class),
                        endpointThis);
            }
        };
    }

}
