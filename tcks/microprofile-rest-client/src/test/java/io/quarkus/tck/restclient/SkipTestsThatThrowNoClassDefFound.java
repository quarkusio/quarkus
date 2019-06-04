package io.quarkus.tck.restclient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.rest.client.tck.ClientHeaderParamTest;
import org.eclipse.microprofile.rest.client.tck.InvokeWithJsonBProviderTest;
import org.eclipse.microprofile.rest.client.tck.InvokeWithJsonPProviderTest;
import org.testng.IAnnotationTransformer;
import org.testng.IConfigurable;
import org.testng.IConfigureCallBack;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

public class SkipTestsThatThrowNoClassDefFound implements IConfigurable, IAnnotationTransformer {

    private static final Set<String> TESTS_THAT_THROW_CLASS_NOT_FOUND = new HashSet<>(Arrays.asList(
            InvokeWithJsonBProviderTest.class.getName(), InvokeWithJsonPProviderTest.class.getName(),
            ClientHeaderParamTest.class.getName()));

    // ensures that the methods annotated with @BeforeTest don't run (since this is where the exception is thrown)
    @Override
    public void run(IConfigureCallBack callBack, ITestResult testResult) {
        ITestNGMethod method = testResult.getMethod();
        if (TESTS_THAT_THROW_CLASS_NOT_FOUND.contains(method.getTestClass().getName())) {
            return;
        }

        callBack.runConfigurationMethod(testResult);
    }

    // ensures the actual tests method don't run
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        String className = testClass != null ? testClass.getName() : testMethod.getDeclaringClass().getName();
        if (TESTS_THAT_THROW_CLASS_NOT_FOUND.contains(className)) {
            annotation.setEnabled(false);
        }
    }
}
