package io.quarkus.it.main.testing.repro8446;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

public class GreeterExtension implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod().map(method -> {
            return Arrays.asList(method.getParameterTypes()).contains(Greeter.class);
        }).orElse(false);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return Stream.of(new HelloTestTemplateInvocationContext(() -> "hello"));
    }

    private static class HelloTestTemplateInvocationContext implements TestTemplateInvocationContext, ParameterResolver {
        private final Greeter greeter;

        public HelloTestTemplateInvocationContext(Greeter greeter) {
            this.greeter = greeter;
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return Collections.singletonList(this);
        }

        @Override
        public boolean supportsParameter(ParameterContext pc, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return pc.getParameter().getType() == Greeter.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            return greeter;
        }
    }
}
