package io.quarkus.smallrye.faulttolerance.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

public class FaultToleranceOperationsDevConsoleSupplier implements Supplier<List<FaultToleranceOperation>> {
    @Override
    public List<FaultToleranceOperation> get() {
        QuarkusFaultToleranceOperationProvider provider = Arc.container()
                .select(QuarkusFaultToleranceOperationProvider.class).get();
        List<FaultToleranceOperation> operations = new ArrayList<>(provider.getOperationCache().values());
        operations.sort(Comparator.comparing(FaultToleranceOperation::getName));
        for (FaultToleranceOperation operation : operations) {
            operation.materialize();
        }
        return operations;
    }
}
