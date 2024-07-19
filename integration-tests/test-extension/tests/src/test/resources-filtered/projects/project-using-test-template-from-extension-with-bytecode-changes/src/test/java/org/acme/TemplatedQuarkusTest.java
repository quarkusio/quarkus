package org.acme;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TemplatedQuarkusTest {

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void verificationTestTemplate(ExtensionContext context) {
        Annotation[] contextAnnotations = context.getRequiredTestClass().getAnnotations();
        Annotation[] myAnnotations = this.getClass().getAnnotations();

        Assertions.assertEquals(myAnnotations.length, contextAnnotations.length,
                "The test template sees a different version of the class than the test execution"
                        + Arrays.toString(myAnnotations) + " (execution) vs " + Arrays.toString(contextAnnotations)
                        + " (context)");
    }

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void classloaderIntrospectionTestTemplate(ExtensionContext context) {
        ClassLoader loader = this.getClass().getClassLoader();
        ClassLoader contextLoader = context.getRequiredTestClass().getClassLoader();

        Assertions.assertEquals(loader, contextLoader,
                "The test template is using a different classloader to the actual test. Execution loader: " + loader
                        + " vs template context loader " + contextLoader);
    }

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void contextAnnotationCheckingTestTemplate(ExtensionContext context) {
        Annotation[] contextAnnotations = context.getRequiredTestClass().getAnnotations();
        Assertions.assertTrue(Arrays.toString(contextAnnotations).contains("AnnotationAddedByExtension"),
                "The JUnit extension context does not see the annotation, only sees " + Arrays.toString(contextAnnotations));
    }

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void executionAnnotationCheckingTestTemplate(ExtensionContext context) {
        Annotation[] myAnnotations = this.getClass().getAnnotations();
        Assertions.assertTrue(Arrays.toString(myAnnotations).contains("AnnotationAddedByExtension"),
                "The test execution does not see the annotation, only sees " + Arrays.toString(myAnnotations)
                        + ". The classloader is " + this.getClass().getClassLoader());
    }
}
