package io.quarkus.arc.processor.bcextensions;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;

abstract class ExtensionPhaseBase {
    private final ExtensionPhase phase;

    final ExtensionInvoker util;
    // this is the application index in @Discovery and the bean archive index in subsequent phases
    final org.jboss.jandex.IndexView index;
    final SharedErrors errors;

    ExtensionPhaseBase(ExtensionPhase phase, ExtensionInvoker util, org.jboss.jandex.IndexView index, SharedErrors errors) {
        this.phase = phase;

        this.util = util;
        this.index = index;
        this.errors = errors;
    }

    final void run() {
        try {
            List<ExtensionMethod> extensionMethods = util.findExtensionMethods(phase.annotationName);

            for (ExtensionMethod method : extensionMethods) {
                runExtensionMethod(method);
            }
        } catch (DefinitionException | DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
    }

    // complex phases may override, but this is enough for the simple phases
    void runExtensionMethod(ExtensionMethod method) throws ReflectiveOperationException {
        List<ExtensionMethodParameter> parameters = new ArrayList<>(method.parametersCount());
        for (org.jboss.jandex.Type parameterType : method.parameterTypes()) {
            ExtensionMethodParameter parameter = ExtensionMethodParameter.of(parameterType);
            parameters.add(parameter);

            parameter.verifyAvailable(phase, method);
        }

        List<Object> arguments = new ArrayList<>(method.parametersCount());
        for (ExtensionMethodParameter parameter : parameters) {
            Object argument = argumentForExtensionMethod(parameter, method);
            arguments.add(argument);
        }

        util.callExtensionMethod(method, arguments);
    }

    // all phases should override and use this as a fallback
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        if (type == ExtensionMethodParameter.MESSAGES) {
            return new MessagesImpl(errors, method.extensionClass);
        }

        throw new IllegalArgumentException("internal error, " + type + " parameter declared at " + method);
    }
}
