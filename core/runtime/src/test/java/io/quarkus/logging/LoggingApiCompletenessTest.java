package io.quarkus.logging;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.jboss.logging.BasicLogger;
import org.junit.jupiter.api.Test;

public class LoggingApiCompletenessTest {
    @Test
    public void compareWithJbossLogging() {
        Method[] jbossLoggingMethods = BasicLogger.class.getDeclaredMethods();
        Method[] quarkusLogMethods = Arrays.stream(Log.class.getDeclaredMethods())
                .filter(Predicate.not(LoggingApiCompletenessTest::isPrivateStaticFail))
                .toArray(Method[]::new);

        List<String> mismatches = new ArrayList<>();

        for (Method jbossLoggingMethod : jbossLoggingMethods) {
            boolean match = false;
            for (Method quarkusLogMethod : quarkusLogMethods) {
                if (areEquivalent(jbossLoggingMethod, quarkusLogMethod)) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                mismatches.add("JBoss Logging method [" + jbossLoggingMethod
                        + "] doesn't have an equivalent in " + Log.class.getName());
            }
        }

        for (Method quarkusLogMethod : quarkusLogMethods) {
            boolean match = false;
            for (Method jbossLoggingMethod : jbossLoggingMethods) {
                if (areEquivalent(jbossLoggingMethod, quarkusLogMethod)) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                mismatches.add(Log.class.getName() + " method [" + quarkusLogMethod
                        + "] doesn't have an equivalent in JBoss Logging");
            }
        }

        if (!mismatches.isEmpty()) {
            mismatches.add(0, "Mismatch between JBoss Logging and " + Log.class.getName() + " detected, "
                    + "use GenerateLog and GenerateAllLogUsages to regenerate the Log class and the integration test");
            fail(String.join("\n- ", mismatches));
        }
    }

    private static boolean isPrivateStaticFail(Method method) {
        return Modifier.isPrivate(method.getModifiers())
                && Modifier.isStatic(method.getModifiers())
                && "fail".equals(method.getName());
    }

    private static boolean areEquivalent(Method jbossLoggingMethod, Method quarkusLogMethod) {
        return jbossLoggingMethod.getName().equals(quarkusLogMethod.getName())
                && jbossLoggingMethod.getReturnType().equals(quarkusLogMethod.getReturnType())
                && Arrays.equals(jbossLoggingMethod.getParameterTypes(), quarkusLogMethod.getParameterTypes());
    }
}
