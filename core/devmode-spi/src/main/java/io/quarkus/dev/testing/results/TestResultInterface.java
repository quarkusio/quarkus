package io.quarkus.dev.testing.results;

import java.util.List;

public interface TestResultInterface {
    List<String> getLogOutput();

    String getDisplayName();

    String getTestClass();

    List<String> getTags();

    boolean isTest();

    String getId();

    long getRunId();

    long getTime();

    List<Throwable> getProblems();

    boolean isReportable();

    State getState();

    enum State {
        PASSED,
        FAILED,
        SKIPPED
    }

}
