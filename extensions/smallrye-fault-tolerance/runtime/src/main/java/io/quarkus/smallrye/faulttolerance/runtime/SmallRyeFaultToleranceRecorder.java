package io.quarkus.smallrye.faulttolerance.runtime;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Recorder
public class SmallRyeFaultToleranceRecorder {

    public void createFaultToleranceOperation(Set<String> beanNames) {
        List<Throwable> allExceptions = new ArrayList<>();
        Map<QuarkusFaultToleranceOperationProvider.CacheKey, FaultToleranceOperation> operationCache = new HashMap<>(
                beanNames.size());
        for (String beanName : beanNames) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader == null) {
                    classLoader = SmallRyeFaultToleranceRecorder.class.getClassLoader();
                }
                Class<?> beanClass = Class.forName(beanName, true, classLoader);

                for (Method method : getAllMethods(beanClass)) {
                    FaultToleranceOperation operation = FaultToleranceOperation.of(beanClass, method);
                    if (operation.isLegitimate()) {
                        try {
                            operation.validate();

                            // register the operation at validation time to avoid re-creating it at runtime
                            QuarkusFaultToleranceOperationProvider.CacheKey cacheKey = new QuarkusFaultToleranceOperationProvider.CacheKey(
                                    beanClass, method);
                            operationCache.put(cacheKey, operation);
                        } catch (FaultToleranceDefinitionException e) {
                            allExceptions.add(e);
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        if (!allExceptions.isEmpty()) {
            if (allExceptions.size() == 1) {
                Throwable error = allExceptions.get(0);
                if (error instanceof DeploymentException) {
                    throw (DeploymentException) error;
                } else {
                    throw new DeploymentException(allExceptions.get(0));
                }
            } else {
                StringBuilder message = new StringBuilder("Found " + allExceptions.size() + " deployment problems: ");
                int idx = 1;
                for (Throwable error : allExceptions) {
                    message.append("\n").append("[").append(idx++).append("] ").append(error.getMessage());
                }
                DeploymentException deploymentException = new DeploymentException(message.toString());
                for (Throwable error : allExceptions) {
                    deploymentException.addSuppressed(error);
                }
                throw deploymentException;
            }
        }
        Arc.container().instance(QuarkusFaultToleranceOperationProvider.class).get().init(operationCache);
    }

    private Set<Method> getAllMethods(Class<?> beanClass) {
        Set<Method> allMethods = new HashSet<>();
        Class<?> currentClass = beanClass;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (Method m : currentClass.getDeclaredMethods()) {
                allMethods.add(m);
            }
            currentClass = currentClass.getSuperclass(); // this will be null for interfaces
        }
        return allMethods;
    }

    public void initExistingCircuitBreakerNames(Set<String> names) {
        Arc.container().instance(QuarkusExistingCircuitBreakerNames.class).get().init(names);
    }
}
