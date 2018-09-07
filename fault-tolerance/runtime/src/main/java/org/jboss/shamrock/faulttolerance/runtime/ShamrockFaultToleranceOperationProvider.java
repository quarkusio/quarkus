package org.jboss.shamrock.faulttolerance.runtime;

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;

import io.smallrye.faulttolerance.FaultToleranceOperationProvider;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Dependent
@Alternative
@Priority(1)
public class ShamrockFaultToleranceOperationProvider implements FaultToleranceOperationProvider {

    @Override
    public FaultToleranceOperation apply(Method method) {
        return FaultToleranceOperation.of(method);
    }

}
