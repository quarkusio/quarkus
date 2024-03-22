package io.quarkus.websockets.next.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketServerException;

class PathParamCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        String paramName = getParamName(context);
        if (paramName != null) {
            if (!context.parameter().type().name().equals(WebSocketDotNames.STRING)) {
                throw new WebSocketServerException("Method parameter annotated with @PathParam must be java.lang.String: "
                        + WebSocketServerProcessor.callbackToString(context.parameter().method()));
            }
            List<String> pathParams = getPathParamNames(context.endpointPath());
            if (!pathParams.contains(paramName)) {
                throw new WebSocketServerException(
                        String.format(
                                "@PathParam name [%s] must be used in the endpoint path [%s]: %s", paramName,
                                context.endpointPath(),
                                WebSocketServerProcessor.callbackToString(context.parameter().method())));
            }
            return true;
        }
        return false;
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        ResultHandle connection = context.getConnection();
        String paramName = getParamName(context);
        return context.bytecode().invokeInterfaceMethod(
                MethodDescriptor.ofMethod(WebSocketConnection.class, "pathParam", String.class, String.class), connection,
                context.bytecode().load(paramName));
    }

    private String getParamName(ParameterContext context) {
        AnnotationInstance pathParamAnnotation = Annotations.find(context.parameterAnnotations(), WebSocketDotNames.PATH_PARAM);
        if (pathParamAnnotation != null) {
            String paramName;
            AnnotationValue nameVal = pathParamAnnotation.value();
            if (nameVal != null) {
                paramName = nameVal.asString();
            } else {
                // Try to use the element name
                paramName = context.parameter().name();
            }
            if (paramName == null) {
                throw new WebSocketServerException(String.format(
                        "Unable to extract the path parameter name - method parameter names not recorded for %s: compile the class with -parameters",
                        context.parameter().method().declaringClass().name()));
            }
            return paramName;
        }
        return null;
    }

    static List<String> getPathParamNames(String path) {
        List<String> names = new ArrayList<>();
        Matcher m = WebSocketServerProcessor.TRANSLATED_PATH_PARAM_PATTERN.matcher(path);
        while (m.find()) {
            names.add(m.group().substring(1));
        }
        return names;
    }

}
