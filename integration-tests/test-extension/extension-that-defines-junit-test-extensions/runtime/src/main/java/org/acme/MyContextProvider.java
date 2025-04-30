package org.acme;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

public class MyContextProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        // TODO In an ideal world, this template context would also see the updated class. At the moment it doesn't,
        // which can be confirmed by uncommenting the following assertion. This class is loaded with an augmentation
        // classloader, and the test class gets a deployment classloader

        //        Annotation[] myAnnotations = extensionContext.getRequiredTestClass().getAnnotations();
        //        Assertions.assertTrue(Arrays.toString(myAnnotations).contains("AnnotationAddedByExtension"),
        //                "The templating context provider does not see the annotation, only sees " + Arrays.toString(myAnnotations)
        //                        + ". The classloader of the checking class is " + this.getClass().getClassLoader()
        //                        + ".\n The classloader of the test class is "
        //                        + extensionContext.getRequiredTestClass().getClassLoader());

        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        Class testClass = extensionContext.getTestClass().get();

        return Stream.of(
                context(new AnnotationCountingTestCase(testClass.getAnnotations(), testClass.getClassLoader())));

    }

    private TestTemplateInvocationContext context(AnnotationCountingTestCase testCase) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return testCase.getDisplayString();
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return asList(
                        new ParameterResolver() {
                            @Override
                            public boolean supportsParameter(ParameterContext parameterContext,
                                    ExtensionContext extensionContext) throws ParameterResolutionException {
                                return true;
                            }

                            @Override
                            public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                                    throws ParameterResolutionException {
                                return extensionContext;
                            }
                        });
            }

        };
    }
}
