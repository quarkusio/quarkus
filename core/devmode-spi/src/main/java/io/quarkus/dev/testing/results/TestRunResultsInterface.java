package io.quarkus.dev.testing.results;

import java.util.Map;

public interface TestRunResultsInterface {
    long getId();

    Map<String, ? extends TestClassResultInterface> getResults();

    long getStartedTime();

    long getCompletedTime();

    long getTotalTime();

    long getPassedCount();

    long getFailedCount();

    long getSkippedCount();

    long getCurrentPassedCount();

    long getCurrentFailedCount();

    long getCurrentSkippedCount();

    long getTotalCount();

    long getCurrentTotalCount();
}
