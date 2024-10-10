package io.quarkus.dev.testing.results;

import java.util.List;

public interface TestClassResultInterface extends Comparable<TestClassResultInterface> {

    String getClassName();

    List<? extends TestResultInterface> getResults();

    @Override
    default int compareTo(TestClassResultInterface o) {
        return getClassName().compareTo(o.getClassName());
    }

}
