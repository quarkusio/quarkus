package org.acme;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

// No QuarkusTest annotation

/**
 * It's likely we would never expect Quarkus bytecode changes to be visible in this kind of test; unit tests which don't have
 * annotation would not be able to
 * benefit from bytecode manipulations from extensions.
 */
public class TemplatedNormalTest {

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void verificationTestTemplate(ExtensionContext context) {
        Annotation[] contextAnnotations = context.getRequiredTestClass()
                .getAnnotations();
        Annotation[] myAnnotations = this.getClass()
                .getAnnotations();

        Assertions.assertEquals(myAnnotations.length, contextAnnotations.length,
                "The test template sees a different version of the class than the test execution"
                        + Arrays.toString(myAnnotations) + " vs " + Arrays.toString(
                                contextAnnotations));
    }

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void classloaderIntrospectionTestTemplate(ExtensionContext context) {
        ClassLoader loader = this.getClass()
                .getClassLoader();
        ClassLoader contextLoader = context.getRequiredTestClass()
                .getClassLoader();

        Assertions.assertEquals(loader, contextLoader,
                "The test template is using a different classloader to the actual test.");
    }

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void contextAnnotationCheckingTestTemplate(ExtensionContext context) {
        // We don't expect to see the annotations because we don't have a @QuarkusTest annotation, but the basic test should work
        Assertions.assertEquals(true, true);
    }

}
