package io.quarkus.deployment.dev.testing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

public class TestState {

    final Map<String, Map<UniqueId, TestResult>> resultsByClass = new HashMap<>();
    final Set<UniqueId> failing = new HashSet<>();
    final Set<UniqueId> dynamicIds = new HashSet<>();

    public static TestState merge(Collection<TestState> states) {
        TestState ret = new TestState();
        for (var i : states) {
            ret.resultsByClass.putAll(i.resultsByClass);
            ret.failing.addAll(i.failing);
            ret.dynamicIds.addAll(i.dynamicIds);
        }
        return ret;
    }

    public List<String> getClassNames() {
        return new ArrayList<>(resultsByClass.keySet()).stream().sorted().collect(Collectors.toList());
    }

    public List<TestClassResult> getPassingClasses() {
        List<TestClassResult> ret = new ArrayList<>();
        for (Map.Entry<String, Map<UniqueId, TestResult>> i : resultsByClass.entrySet()) {
            List<TestResult> passing = new ArrayList<>();
            List<TestResult> failing = new ArrayList<>();
            List<TestResult> skipped = new ArrayList<>();
            long time = 0;
            for (TestResult j : i.getValue().values()) {
                if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(j);
                } else if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                    skipped.add(j);
                } else {
                    passing.add(j);
                }
                if (j.getUniqueId().getLastSegment().getType().equals("class")) {
                    time = j.getTime();
                }
            }
            if (failing.isEmpty()) {
                TestClassResult p = new TestClassResult(i.getKey(), passing, failing, skipped, time);
                ret.add(p);
            }
        }

        Collections.sort(ret);
        return ret;
    }

    public List<TestClassResult> getFailingClasses() {
        List<TestClassResult> ret = new ArrayList<>();
        for (Map.Entry<String, Map<UniqueId, TestResult>> i : resultsByClass.entrySet()) {
            long time = 0;
            List<TestResult> passing = new ArrayList<>();
            List<TestResult> failing = new ArrayList<>();
            List<TestResult> skipped = new ArrayList<>();
            for (TestResult j : i.getValue().values()) {
                if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(j);
                } else if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                    skipped.add(j);
                } else {
                    passing.add(j);
                }
                if (j.getUniqueId().getLastSegment().getType().equals("class")) {
                    time = j.getTime();
                }
            }
            if (!failing.isEmpty()) {
                TestClassResult p = new TestClassResult(i.getKey(), passing, failing, skipped, time);
                ret.add(p);
            }
        }
        Collections.sort(ret);
        return ret;
    }

    public synchronized void updateResults(Map<String, Map<UniqueId, TestResult>> latest) {
        for (Map.Entry<String, Map<UniqueId, TestResult>> entry : latest.entrySet()) {
            Map<UniqueId, TestResult> existing = this.resultsByClass.get(entry.getKey());
            if (existing == null) {
                resultsByClass.put(entry.getKey(), entry.getValue());
            } else {
                existing.putAll(entry.getValue());
            }
            for (Map.Entry<UniqueId, TestResult> r : entry.getValue().entrySet()) {
                if (r.getValue().getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(r.getKey());
                } else {
                    failing.remove(r.getKey());
                }
            }
        }
    }

    public synchronized void classesRemoved(Set<String> classNames) {
        for (String i : classNames) {
            resultsByClass.remove(i);
        }
    }

    public Map<String, Map<UniqueId, TestResult>> getCurrentResults() {
        return Collections.unmodifiableMap(resultsByClass);
    }

    public int getTotalFailures() {
        int count = 0;
        for (Map<UniqueId, TestResult> i : resultsByClass.values()) {
            for (TestResult j : i.values()) {
                if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<TestResult> getHistoricFailures(Map<String, Map<UniqueId, TestResult>> currentResults) {
        List<TestResult> ret = new ArrayList<>();
        for (Map.Entry<String, Map<UniqueId, TestResult>> entry : resultsByClass.entrySet()) {
            for (TestResult j : entry.getValue().values()) {
                if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    if (currentResults.containsKey(entry.getKey())) {
                        if (currentResults.get(entry.getKey()).containsKey(j.getUniqueId())) {
                            continue;
                        }
                    }
                    ret.add(j);
                }
            }
        }
        return ret;
    }

    public boolean isFailed(TestDescriptor testDescriptor) {
        return failing.contains(testDescriptor.getUniqueId());
    }

    public void pruneDeletedTests(Set<UniqueId> allDiscoveredIds, Set<UniqueId> dynamicIds) {
        Set<UniqueId> dynamicParents = dynamicIds.stream().map(UniqueId::removeLastSegment).collect(Collectors.toSet());
        this.dynamicIds.removeIf(s -> {
            //was actually run, don't remove
            if (dynamicIds.contains(s)) {
                return false;
            }
            UniqueId parent = s.removeLastSegment();
            //parent was run, but not this test, so it has been removed
            if (dynamicParents.contains(parent)) {
                return true;
            }
            return !allDiscoveredIds.contains(parent);
        });
        this.dynamicIds.addAll(dynamicIds);
        failing.removeIf(i -> (!allDiscoveredIds.contains(i) && !this.dynamicIds.contains(i)));
        for (Map.Entry<String, Map<UniqueId, TestResult>> cr : resultsByClass.entrySet()) {
            cr.getValue().entrySet()
                    .removeIf(s -> (!allDiscoveredIds.contains(s.getKey()) && !this.dynamicIds.contains(s.getKey())));
        }
        resultsByClass.entrySet().removeIf(s -> s.getValue().isEmpty());
    }
}
