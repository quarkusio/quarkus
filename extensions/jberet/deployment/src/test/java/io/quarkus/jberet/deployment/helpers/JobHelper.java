package io.quarkus.jberet.deployment.helpers;

import static javax.batch.runtime.BatchStatus.ABANDONED;
import static javax.batch.runtime.BatchStatus.COMPLETED;
import static javax.batch.runtime.BatchStatus.FAILED;
import static javax.batch.runtime.BatchStatus.STOPPED;

import java.util.EnumSet;
import java.util.concurrent.TimeoutException;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

public class JobHelper {

    public static BatchStatus waitForExecutionFinish(Long executionId) throws TimeoutException {
        return waitForExecutionFinish(executionId, 20L);
    }

    public static BatchStatus waitForExecutionFinish(Long executionId, Long timeoutSeconds) throws TimeoutException {
        long start = System.currentTimeMillis();
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        while ((System.currentTimeMillis() - start) / 1000 < timeoutSeconds) {
            BatchStatus currentStatus = jobOperator.getJobExecution(executionId).getBatchStatus();
            if (EnumSet.of(COMPLETED, FAILED, ABANDONED, STOPPED).contains(currentStatus)) {
                return currentStatus;
            }
        }
        throw new TimeoutException();
    }

}
