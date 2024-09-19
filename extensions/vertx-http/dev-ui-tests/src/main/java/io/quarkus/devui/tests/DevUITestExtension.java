package io.quarkus.devui.tests;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class DevUITestExtension implements ParameterResolver {

    private static boolean isJsonRPCServiceClient(ParameterContext parameterContext) {
        return parameterContext.getParameter().getType() == JsonRPCServiceClient.class;
    }

    private static boolean isDevUiResourceResolver(ParameterContext parameterContext) {
        return parameterContext.getParameter().getType() == DevUiResourceResolver.class;
    }

    private static boolean isBuildTimeDataResolver(ParameterContext parameterContext) {
        return parameterContext.getParameter().getType() == BuildTimeDataResolver.class;
    }

    private static <T extends Annotation> Optional<T> findAnnotation(ExtensionContext context, Class<T> annotationType) {
        final var methodAnnotation = context
                .getTestMethod() // could be constructor injection -> no test method
                .map(method -> method.getAnnotation(annotationType))
                .orElse(null);
        if (null != methodAnnotation) {
            return Optional.of(methodAnnotation);
        } else {
            return Optional.ofNullable(
                    context
                            .getRequiredTestClass()
                            .getAnnotation(annotationType));
        }
    }

    private static String findNameSpace(
            ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        return parameterContext
                .findAnnotation(Namespace.class)
                .orElseGet(
                        () -> findAnnotation(extensionContext, DevUITest.class)
                                .map(DevUITest::value)
                                .orElseThrow(() -> new IllegalStateException("No @Namespace annotation found.")))
                .value();
    }

    private static DevUiResourceResolver createDevUiResourceResolver(
            ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        return findAnnotation(extensionContext, DevUITest.class)
                .map(DevUITest::host)
                .filter(Predicate.not(String::isEmpty))
                .map(DevUiResourceResolver::new)
                .orElseGet(DevUiResourceResolver::new);
    }

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext) throws ParameterResolutionException {
        return isDevUiResourceResolver(parameterContext)
                || isJsonRPCServiceClient(parameterContext)
                || isBuildTimeDataResolver(parameterContext);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext) throws ParameterResolutionException {
        if (isJsonRPCServiceClient(parameterContext)) {
            return new JsonRPCServiceClient(
                    findNameSpace(parameterContext, extensionContext),
                    createDevUiResourceResolver(parameterContext, extensionContext));
        }
        if (isBuildTimeDataResolver(parameterContext)) {
            return new BuildTimeDataResolver(
                    findNameSpace(parameterContext, extensionContext),
                    createDevUiResourceResolver(parameterContext, extensionContext));
        }
        if (isDevUiResourceResolver(parameterContext)) {
            return new DevUiResourceResolver(findNameSpace(parameterContext, extensionContext));
        }
        throw new IllegalStateException("Unsupported parameter type: " + parameterContext.getParameter().getType());
    }

}
