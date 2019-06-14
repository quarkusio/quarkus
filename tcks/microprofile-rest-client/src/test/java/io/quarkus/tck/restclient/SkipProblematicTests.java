package io.quarkus.tck.restclient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.rest.client.tck.ClientHeaderParamTest;
import org.eclipse.microprofile.rest.client.tck.InvokeWithJsonBProviderTest;
import org.eclipse.microprofile.rest.client.tck.InvokeWithJsonPProviderTest;
import org.eclipse.microprofile.rest.client.tck.asynctests.AsyncMethodTest;
import org.testng.IAnnotationTransformer;
import org.testng.IConfigurable;
import org.testng.IConfigureCallBack;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

public class SkipProblematicTests implements IConfigurable, IAnnotationTransformer {

    private static final Set<String> TESTS_THAT_THROW_CLASS_NOT_FOUND = new HashSet<>(Arrays.asList(
            InvokeWithJsonBProviderTest.class.getName(), InvokeWithJsonPProviderTest.class.getName(),
            ClientHeaderParamTest.class.getName()));

    private static final Set<String> RACEY_TESTS = new HashSet<>(Arrays.asList(
            //testAsyncInvocationInterceptorProvider is racey, as there is no guarantee the
            //removeThreadId has been set by the time the test attempts to assert it, this is
            //because the removeContext action happens after the response is provided, so the
            //completion stage is complete before this method is called
            AsyncMethodTest.class.getName()));

    private static final Set<String> SKIP;

    static {
        SKIP = new HashSet<>();
        SKIP.addAll(TESTS_THAT_THROW_CLASS_NOT_FOUND);
        SKIP.addAll(RACEY_TESTS);
    }

    // ensures that the methods annotated with @BeforeTest don't run (since this is where the exception is thrown)
    @Override
    public void run(IConfigureCallBack callBack, ITestResult testResult) {
        ITestNGMethod method = testResult.getMethod();
        if (SKIP.contains(method.getTestClass().getName())) {
            return;
        }

        callBack.runConfigurationMethod(testResult);
    }

    // ensures the actual tests method don't run
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        String className = testClass != null ? testClass.getName() : testMethod.getDeclaringClass().getName();
        if (SKIP.contains(className)) {
            annotation.setEnabled(false);
        }
    }
}
