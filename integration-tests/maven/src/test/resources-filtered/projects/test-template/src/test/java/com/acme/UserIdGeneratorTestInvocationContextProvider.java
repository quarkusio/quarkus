package com.acme;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

public class UserIdGeneratorTestInvocationContextProvider implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        return Stream.of(
                genericContext(new UserIdGeneratorTestCase()),
                genericContext(new UserIdGeneratorTestCase()));
    }

    private TestTemplateInvocationContext genericContext(
            UserIdGeneratorTestCase userIdGeneratorTestCase) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return userIdGeneratorTestCase.getDisplayName();
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return Arrays.asList(parameterResolver(), preProcessor(), postProcessor());
            }

            private BeforeTestExecutionCallback preProcessor() {
                return context -> System.out.println("Pre-process parameter: " + userIdGeneratorTestCase.getDisplayName());
            }

            private AfterTestExecutionCallback postProcessor() {
                return context -> System.out.println("Post-process parameter: " + userIdGeneratorTestCase.getDisplayName());
            }

            private ParameterResolver parameterResolver() {
                return new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
                        return parameterContext.getParameter()
                                .getType()
                                .equals(UserIdGeneratorTestCase.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
                        return userIdGeneratorTestCase;
                    }
                };
            }
        };
    }
}
