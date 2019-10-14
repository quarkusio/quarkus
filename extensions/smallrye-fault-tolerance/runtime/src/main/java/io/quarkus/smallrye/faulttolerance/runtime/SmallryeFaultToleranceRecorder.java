package io.quarkus.smallrye.faulttolerance.runtime;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.metric.HystrixCollapserEventStream;
import com.netflix.hystrix.metric.HystrixCommandCompletionStream;
import com.netflix.hystrix.metric.HystrixCommandStartStream;
import com.netflix.hystrix.metric.HystrixThreadEventStream;
import com.netflix.hystrix.metric.HystrixThreadPoolCompletionStream;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Recorder
public class SmallryeFaultToleranceRecorder {

    public void resetCommandContextOnUndeploy(ShutdownContext context) {
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                HystrixCommandCompletionStream.reset();
                HystrixCollapserEventStream.reset();
                HystrixCommandStartStream.reset();
                HystrixThreadPoolCompletionStream.reset();
                HystrixCommandStartStream.reset();
                HystrixThreadEventStream.getInstance().shutdown();
                Hystrix.reset();
            }
        });
    }

    public void validate(List<String> beanNames) {
        List<Throwable> allExceptions = new ArrayList<>();
        for (String beanName : beanNames) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader == null) {
                    classLoader = SmallryeFaultToleranceRecorder.class.getClassLoader();
                }
                Class<?> beanClass = Class.forName(beanName, true, classLoader);

                for (Method method : getMethodsForValidation(beanClass)) {
                    FaultToleranceOperation operation = FaultToleranceOperation.of(beanClass, method);
                    if (operation.isLegitimate()) {
                        try {
                            operation.validate();
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
    }

    private Set<Method> getMethodsForValidation(Class<?> beanClass) {
        Set<Method> allMethods = new HashSet<>();
        Class<?> currentClass = beanClass;
        while (!currentClass.equals(Object.class)) {
            for (Method m : currentClass.getDeclaredMethods()) {
                allMethods.add(m);
            }
            currentClass = currentClass.getSuperclass();
        }
        return allMethods;
    }
}
