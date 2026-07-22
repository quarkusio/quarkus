package io.quarkus.arquillian;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.impl.client.deployment.AnnotationDeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;

/**
 * Filters out inherited {@link Deployment} methods that are hidden by the concrete test class's
 * own methods with the same signature. This prevents deploying unused parent deployments which
 * would each start a separate Quarkus instance unnecessarily.
 */
public class QuarkusDeploymentScenarioGenerator extends AnnotationDeploymentScenarioGenerator {

    @Override
    public List<DeploymentDescription> generate(TestClass testClass) {
        List<DeploymentDescription> descriptions = super.generate(testClass);
        if (descriptions.size() <= 1) {
            return descriptions;
        }

        Class<?> javaClass = testClass.getJavaClass();
        Set<String> ownSignatures = new HashSet<>();
        for (Method m : javaClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Deployment.class)) {
                ownSignatures.add(methodSignature(m));
            }
        }

        if (ownSignatures.isEmpty()) {
            return descriptions;
        }

        boolean hasHiddenMethods = false;
        Class<?> parent = javaClass.getSuperclass();
        while (parent != null && parent != Object.class) {
            for (Method m : parent.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Deployment.class) && ownSignatures.contains(methodSignature(m))) {
                    hasHiddenMethods = true;
                    break;
                }
            }
            if (hasHiddenMethods) {
                break;
            }
            parent = parent.getSuperclass();
        }

        if (hasHiddenMethods) {
            return descriptions.subList(0, ownSignatures.size());
        }
        return descriptions;
    }

    private static String methodSignature(Method m) {
        return m.getName() + Arrays.toString(m.getParameterTypes());
    }
}
