package io.quarkus.deployment.dev.testing;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.PostDiscoveryFilter;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;

public class ModuleTestRunner {

    final TestState testState = new TestState();
    private final TestSupport testSupport;
    private final DevModeContext devModeContext;
    private final CuratedApplication testApplication;
    private final DevModeContext.ModuleInfo moduleInfo;

    private final TestClassUsages testClassUsages = new TestClassUsages();
    private JunitTestRunner runner;

    public ModuleTestRunner(TestSupport testSupport, DevModeContext devModeContext, CuratedApplication testApplication,
            DevModeContext.ModuleInfo moduleInfo) {
        this.testSupport = testSupport;
        this.devModeContext = devModeContext;
        this.testApplication = testApplication;
        this.moduleInfo = moduleInfo;
    }

    public synchronized void abort() {
        notifyAll();
        if (runner != null) {
            runner.abort();
        }
    }

    Runnable prepare(ClassScanResult classScanResult, boolean reRunFailures, long runId, TestRunListener listener) {

        var old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(testApplication.getAugmentClassLoader());
        try {
            synchronized (this) {
                if (runner != null) {
                    throw new IllegalStateException("Tests already in progress");
                }
                JunitTestRunner.Builder builder = new JunitTestRunner.Builder()
                        .setClassScanResult(classScanResult)
                        .setDevModeContext(devModeContext)
                        .setRunId(runId)
                        .setTestState(testState)
                        .setTestClassUsages(testClassUsages)
                        .setTestApplication(testApplication)
                        .setIncludeTags(testSupport.includeTags)
                        .setExcludeTags(testSupport.excludeTags)
                        .setInclude(testSupport.include)
                        .setExclude(testSupport.exclude)
                        .setTestType(testSupport.testType)
                        .setModuleInfo(moduleInfo)
                        .addListener(listener)
                        .setFailingTestsOnly(classScanResult != null && testSupport.brokenOnlyMode); //broken only mode is only when changes are made, not for forced runs
                if (reRunFailures) {
                    Set<UniqueId> ids = new HashSet<>();
                    for (Map.Entry<String, TestClassResult> e : testSupport.testRunResults.getCurrentFailing().entrySet()) {
                        for (TestResult test : e.getValue().getFailing()) {
                            ids.add(test.uniqueId);
                        }
                    }
                    builder.addAdditionalFilter(new PostDiscoveryFilter() {
                        @Override
                        public FilterResult apply(TestDescriptor testDescriptor) {
                            return FilterResult.includedIf(ids.contains(testDescriptor.getUniqueId()));
                        }
                    });
                }
                runner = builder
                        .build();
            }
            var prepared = runner.prepare();
            return new Runnable() {
                @Override
                public void run() {
                    var old = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(testApplication.getAugmentClassLoader());
                    try {
                        prepared.run();
                        synchronized (ModuleTestRunner.this) {
                            runner = null;
                        }
                    } finally {
                        Thread.currentThread().setContextClassLoader(old);
                    }
                }
            };
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public TestState getTestState() {
        return testState;
    }
}
