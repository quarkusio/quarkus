package io.quarkus.deployment.dev.testing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

public class TestClassUsages implements Serializable {

    private final Map<ClassAndMethod, Set<String>> classNames = new HashMap<>();

    public synchronized void updateTestData(String currentclass, UniqueId test, Set<String> touched) {
        classNames.put(new ClassAndMethod(currentclass, test), touched);
    }

    public synchronized void updateTestData(String currentclass, Set<String> touched) {
        classNames.put(new ClassAndMethod(currentclass, null), touched);
    }

    public synchronized void merge(TestClassUsages newData) {
        classNames.putAll(newData.classNames);
    }

    public synchronized PostDiscoveryFilter getTestsToRun(Set<String> changedClasses, TestState testState) {

        Set<UniqueId> touchedIds = new HashSet<>();
        //classes that have at least one test
        Set<String> testClassesToRun = new HashSet<>();
        for (Map.Entry<ClassAndMethod, Set<String>> entry : classNames.entrySet()) {
            if (entry.getKey().uniqueId != null) {
                if (changedClasses.contains(entry.getKey().className)) {
                    touchedIds.add(entry.getKey().uniqueId);
                    testClassesToRun.add(entry.getKey().className);
                } else {
                    for (String i : changedClasses) {
                        if (entry.getValue().contains(i)) {
                            touchedIds.add(entry.getKey().uniqueId);
                            testClassesToRun.add(entry.getKey().className);
                            break;
                        }
                    }
                }
            }
        }

        return new PostDiscoveryFilter() {
            @Override
            public FilterResult apply(TestDescriptor testDescriptor) {
                if (testState.isFailed(testDescriptor)) {
                    return FilterResult.included("Test failed previously");
                }
                if (!testDescriptor.getSource().isPresent()) {
                    return FilterResult.included("No source information");
                }
                if (touchedIds.contains(testDescriptor.getUniqueId())) {
                    return FilterResult.included("Class was touched");
                }
                TestSource source = testDescriptor.getSource().get();
                if (source instanceof ClassSource) {
                    String testClassName = ((ClassSource) source).getClassName();
                    ClassAndMethod cm = new ClassAndMethod(testClassName, null);
                    if (!classNames.containsKey(cm)) {
                        return FilterResult.included("No test information");
                    } else if (changedClasses.contains(testClassName)) {
                        return FilterResult.included("Test case was modified");
                    } else if (testClassesToRun.contains(testClassName)) {
                        return FilterResult.included("Has at least one test");
                    } else {
                        return FilterResult.excluded("Has no tests");
                    }
                } else if (source instanceof MethodSource) {
                    MethodSource ms = (MethodSource) source;
                    ClassAndMethod cm = new ClassAndMethod(ms.getClassName(), testDescriptor.getUniqueId());
                    if (!classNames.containsKey(cm)) {
                        return FilterResult.included("No test information");
                    } else if (changedClasses.contains(ms.getClassName())) {
                        return FilterResult.included("Test case was modified");
                    } else if (touchedIds.contains(testDescriptor.getUniqueId())) {
                        return FilterResult.included("Test touches changed classes");
                    } else {
                        return FilterResult.excluded("Test does not need to run");
                    }
                } else {
                    return FilterResult.included("Unknown source type");
                }

            }
        };
    }

    private static final class ClassAndMethod implements Serializable {
        private final String className;
        private final UniqueId uniqueId;

        private ClassAndMethod(String className, UniqueId uniqueId) {
            this.className = className;
            this.uniqueId = uniqueId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ClassAndMethod that = (ClassAndMethod) o;
            return Objects.equals(className, that.className) &&
                    Objects.equals(uniqueId, that.uniqueId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, uniqueId);
        }

        public String getClassName() {
            return className;
        }

        public UniqueId getUniqueId() {
            return uniqueId;
        }
    }
}
