package io.quarkus.websockets.next.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.websockets.next.WebSocketException;
import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;

class PathParamCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        String name = getParamName(context);
        if (name != null) {
            if (!context.parameter().type().name().equals(WebSocketDotNames.STRING)) {
                throw new WebSocketException("Method parameter annotated with @PathParam must be java.lang.String: "
                        + WebSocketProcessor.methodToString(context.parameter().method()));
            }
            if (context.endpointPath() == null) {
                throw new WebSocketException("Global error handlers may not accept @PathParam parameters: "
                        + WebSocketProcessor.methodToString(context.parameter().method()));
            }
            List<String> pathParams = getPathParamNames(context.endpointPath());
            if (!pathParams.contains(name)) {
                throw new WebSocketException(
                        String.format(
                                "@PathParam name [%s] must be used in the endpoint path [%s]: %s", name,
                                context.endpointPath(),
                                WebSocketProcessor.methodToString(context.parameter().method())));
            }
            return true;
        }
        return false;
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        String paramName = getParamName(context);
        return context.bytecode().invokeVirtualMethod(
                MethodDescriptor.ofMethod(WebSocketConnectionBase.class, "pathParam", String.class, String.class),
                context.getConnection(),
                context.bytecode().load(paramName));
    }

    private String getParamName(ParameterContext context) {
        AnnotationInstance pathParamAnnotation = Annotations.find(context.parameterAnnotations(), WebSocketDotNames.PATH_PARAM);
        if (pathParamAnnotation != null) {
            String name;
            AnnotationValue nameVal = pathParamAnnotation.value();
            if (nameVal != null) {
                name = nameVal.asString();
            } else {
                // Try to use the element name
                name = context.parameter().name();
            }
            if (name == null) {
                throw new WebSocketException(String.format(
                        "Unable to extract the path parameter name - method parameter names not recorded for %s: compile the class with -parameters",
                        context.parameter().method().declaringClass().name()));
            }
            return name;
        }
        return null;
    }

    static List<String> getPathParamNames(String path) {
        List<String> names = new ArrayList<>();
        Matcher m = WebSocketProcessor.TRANSLATED_PATH_PARAM_PATTERN.matcher(path);
        while (m.find()) {
            names.add(m.group().substring(1));
        }
        return names;
    }

}
