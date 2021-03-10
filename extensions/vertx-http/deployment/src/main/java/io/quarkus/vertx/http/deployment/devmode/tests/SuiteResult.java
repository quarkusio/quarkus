package io.quarkus.vertx.http.deployment.devmode.tests;

import java.util.Map;

public class SuiteResult {
    private Map<String, ClassResult> results;

    public SuiteResult() {
    }

    public SuiteResult(Map<String, ClassResult> results) {
        this.results = results;
    }

    public Map<String, ClassResult> getResults() {
        return results;
    }

    public SuiteResult setResults(Map<String, ClassResult> results) {
        this.results = results;
        return this;
    }
}
