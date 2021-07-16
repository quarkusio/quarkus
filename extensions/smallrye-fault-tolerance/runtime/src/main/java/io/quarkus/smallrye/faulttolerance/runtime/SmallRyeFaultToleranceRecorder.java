package io.quarkus.smallrye.faulttolerance.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Recorder
public class SmallRyeFaultToleranceRecorder {

    public void createFaultToleranceOperation(List<FaultToleranceMethod> ftMethods) {
        List<Throwable> allExceptions = new ArrayList<>();
        Map<QuarkusFaultToleranceOperationProvider.CacheKey, FaultToleranceOperation> operationCache = new HashMap<>(
                ftMethods.size());
        for (FaultToleranceMethod ftMethod : ftMethods) {
            FaultToleranceOperation operation = FaultToleranceOperation.create(ftMethod);
            try {
                operation.validate();

                QuarkusFaultToleranceOperationProvider.CacheKey cacheKey = new QuarkusFaultToleranceOperationProvider.CacheKey(
                        ftMethod.beanClass, ftMethod.method.reflect());
                operationCache.put(cacheKey, operation);
            } catch (FaultToleranceDefinitionException | NoSuchMethodException e) {
                allExceptions.add(e);
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

    public void initExistingCircuitBreakerNames(Set<String> names) {
        Arc.container().instance(QuarkusExistingCircuitBreakerNames.class).get().init(names);
    }
}
