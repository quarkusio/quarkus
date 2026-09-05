package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.continuoustesting.ContinuousTestingMavenTestUtils;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * Tests for @QuarkusTest, mostly edge cases
 * This should perhaps be in a different project, see https://github.com/quarkusio/quarkus/issues/46667
 */
public class QuarkusTestIT extends RunAndCheckMojoTestBase {

    @Test
    public void testQuarkusTestWithThirdPartyExtensionContinuousTesting()
            throws MavenInvocationException, FileNotFoundException {
        //we also check continuous testing
        String sourceDir = "projects/test-third-party-junit-extension";
        testDir = initProject(sourceDir, sourceDir + "-processed-devmode");

        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(1, results.getTestsPassed());
    }

    @Test
    public void testQuarkusTestWithThirdPartyExtension()
            throws MavenInvocationException, InterruptedException {
        String sourceDir = "projects/test-third-party-junit-extension";
        testDir = initProject(sourceDir, sourceDir + "-processed");
        RunningInvoker invoker = new RunningInvoker(testDir, false);

        // to properly surface the problem of multiple classpath entries, we need to install the project to the local m2
        MavenProcessInvocationResult installInvocation = invoker.execute(
                List.of("clean", "verify", "-Dquarkus.analytics.disabled=true"),
                Collections.emptyMap());
        assertThat(installInvocation.getProcess().waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(installInvocation.getExecutionException()).isNull();
        assertThat(installInvocation.getExitCode()).isEqualTo(0);

    }

    @Test
    public void testNestedQuarkusTestMixedWithNormalTests()
            throws MavenInvocationException, InterruptedException {
        String sourceDir = "projects/test-nested-tests-mixed-with-normal-tests";
        testDir = initProject(sourceDir, sourceDir + "-processed");
        RunningInvoker invoker = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult installInvocation = invoker.execute(
                List.of("clean", "verify", "-Dquarkus.analytics.disabled=true"),
                Collections.emptyMap());
        assertThat(installInvocation.getProcess().waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(installInvocation.getExecutionException()).isNull();
        assertThat(installInvocation.getExitCode()).isEqualTo(0);

    }

    /**
     * When JUnit is going to exclude every test of a {@code @QuarkusTest} class through its tags, no Quarkus
     * application must be built for that class: the application in this project deliberately fails to build (see
     * {@code BrokenBean}), and building it means Dev Services would be started (and Docker required) for tests that
     * never run.
     *
     * See https://github.com/quarkusio/quarkus/issues/54435
     */
    @Test
    public void testQuarkusTestsExcludedByTagsDoNotBuildAnApplication()
            throws MavenInvocationException, InterruptedException, IOException {
        String sourceDir = "projects/test-quarkus-tests-excluded-by-tags";
        testDir = initProject(sourceDir, sourceDir + "-processed");

        // Every test in both @QuarkusTest classes carries the class-level tag (the nested test inherits it)
        RunningInvoker excludedByClassTag = runTests("-DexcludedGroups=quarkus-app");
        assertThat(excludedByClassTag.getResult().getExitCode()).as(excludedByClassTag.log()).isEqualTo(0);
        assertThat(excludedByClassTag.log()).contains("Tests run: 1, Failures: 0, Errors: 0, Skipped: 0")
                .doesNotContain("Unsatisfied dependency");

        // Same thing expressed as an inclusion: only the plain JUnit test is selected
        RunningInvoker includedByTag = runTests("-Dgroups=vanilla");
        assertThat(includedByTag.getResult().getExitCode()).as(includedByTag.log()).isEqualTo(0);
        assertThat(includedByTag.log()).contains("Tests run: 1, Failures: 0, Errors: 0, Skipped: 0")
                .doesNotContain("Unsatisfied dependency");

        // Control: MethodTaggedQuarkusTest still has an untagged test to run, so the application has to be built,
        // which fails. This proves the runs above would have hit the same failure without the exclusion.
        RunningInvoker partiallyExcluded = runTests("-DexcludedGroups=partial");
        assertThat(partiallyExcluded.getResult().getExitCode()).as(partiallyExcluded.log()).isNotEqualTo(0);
        assertThat(partiallyExcluded.log()).contains("Unsatisfied dependency");
    }

    private RunningInvoker runTests(String... args) throws MavenInvocationException, InterruptedException {
        RunningInvoker invoker = new RunningInvoker(testDir, false);
        List<String> goals = new ArrayList<>(List.of("clean", "test", "-Dquarkus.analytics.disabled=true"));
        goals.addAll(List.of(args));
        MavenProcessInvocationResult result = invoker.execute(goals, Collections.emptyMap());
        assertThat(result.getProcess().waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(result.getExecutionException()).isNull();
        return invoker;
    }

    @Test
    public void testNestedQuarkusTestMixedWithNormalTestsContinuousTesting()
            throws MavenInvocationException, FileNotFoundException {
        // This test will fail if the test extension does not reset the TCCL properly
        String sourceDir = "projects/test-nested-tests-mixed-with-normal-tests";
        testDir = initProject(sourceDir, sourceDir + "-processed-devmode");

        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(4, results.getTestsPassed());
        Assertions.assertEquals(1, results.getTestsSkipped());
    }

    /**
     * Tests that if @QuarkusTest is added as a JUnitExtension through META-INF/services, things still work.
     * JBeret does this, for example.
     * * This should perhaps be in a different project, see https://github.com/quarkusio/quarkus/issues/46667
     */
    @Disabled("Not working, see https://github.com/quarkusio/quarkus/issues/46779")
    @Test
    public void testQuarkusTestInMetaInfServicesContinuousTesting()
            throws MavenInvocationException, FileNotFoundException, InterruptedException {
        //we also check continuous testing
        String sourceDir = "projects/quarkustest-added-with-serviceloader";
        File parentDir = initProject(sourceDir, sourceDir + "-processed-devmode");
        testDir = new File(parentDir, "runner");

        {
            // We need to build the extension first, before running the tests as continuous tests
            RunningInvoker invoker = new RunningInvoker(new File(parentDir, "runtime"), false);
            // to properly surface the problem of multiple classpath entries, we need to install the project to the local m2
            MavenProcessInvocationResult installInvocation = invoker.execute(
                    List.of("clean", "install", "-Dquarkus.analytics.disabled=true"),
                    Collections.emptyMap());
            assertThat(installInvocation.getProcess()
                    .waitFor(2, TimeUnit.MINUTES)).isTrue();
            assertThat(installInvocation.getExecutionException()).isNull();
            assertThat(installInvocation.getExitCode()).isEqualTo(0);
        }

        {
            // We need to build the extension first, before running the tests as continuous tests
            RunningInvoker invoker = new RunningInvoker(new File(parentDir, "deployment"), false);
            // to properly surface the problem of multiple classpath entries, we need to install the project to the local m2
            MavenProcessInvocationResult installInvocation = invoker.execute(
                    List.of("clean", "install", "-Dquarkus.analytics.disabled=true"),
                    Collections.emptyMap());
            assertThat(installInvocation.getProcess()
                    .waitFor(2, TimeUnit.MINUTES)).isTrue();
            assertThat(installInvocation.getExecutionException()).isNull();
            assertThat(installInvocation.getExitCode()).isEqualTo(0);
        }

        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(1, results.getTestsPassed());
    }

    @Test
    public void testQuarkusTestInMetaInfServicesNormalTesting()
            throws MavenInvocationException, InterruptedException {
        String sourceDir = "projects/quarkustest-added-with-serviceloader";
        testDir = initProject(sourceDir, sourceDir + "-processed");
        RunningInvoker invoker = new RunningInvoker(testDir, false);

        // to properly surface the problem of multiple classpath entries, we need to install the project to the local m2
        MavenProcessInvocationResult installInvocation = invoker.execute(
                List.of("clean", "verify", "-Dquarkus.analytics.disabled=true"),
                Collections.emptyMap());
        assertThat(installInvocation.getProcess().waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(installInvocation.getExecutionException()).isNull();
        assertThat(installInvocation.getExitCode()).isEqualTo(0);

    }

    @Disabled("Not working, see https://github.com/quarkusio/quarkus/issues/47364")
    @Test
    public void testQuarkusTestGuardedByConditionContinuousTesting()
            throws MavenInvocationException, FileNotFoundException {
        //we also check continuous testing
        String sourceDir = "projects/test-test-conditions";
        testDir = initProject(sourceDir, sourceDir + "-processed-devmode");

        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(1, results.getTestsPassed());
    }

    @Test
    public void testQuarkusTestGuardedByCondition()
            throws MavenInvocationException, InterruptedException {
        String sourceDir = "projects/test-test-conditions";
        testDir = initProject(sourceDir, sourceDir + "-processed");
        RunningInvoker invoker = new RunningInvoker(testDir, false);

        // to properly surface the problem of multiple classpath entries, we need to install the project to the local m2
        MavenProcessInvocationResult installInvocation = invoker.execute(
                List.of("clean", "verify", "-Dquarkus.analytics.disabled=true"),
                Collections.emptyMap());
        assertThat(installInvocation.getProcess().waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(installInvocation.getExecutionException()).isNull();
        assertThat(installInvocation.getExitCode()).isEqualTo(0);

    }

    /*
     * This should perhaps be in a different project, see https://github.com/quarkusio/quarkus/issues/46667
     */
    @Test
    public void testQuarkusTestWithConfigInTestProfileContinuousTesting()
            throws MavenInvocationException, FileNotFoundException {
        //we also check continuous testing
        String sourceDir = "projects/test-config-in-test-profile";
        testDir = initProject(sourceDir, sourceDir + "-processed-devmode");

        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(2, results.getTestsPassed());
    }

    @Test
    public void testQuarkusTestWithConfigInTestProfile()
            throws MavenInvocationException, InterruptedException {
        String sourceDir = "projects/test-config-in-test-profile";
        testDir = initProject(sourceDir, sourceDir + "-processed");
        RunningInvoker invoker = new RunningInvoker(testDir, false);

        // to properly surface the problem of multiple classpath entries, we need to install the project to the local m2
        MavenProcessInvocationResult installInvocation = invoker.execute(
                List.of("clean", "verify", "-Dquarkus.analytics.disabled=true"),
                Collections.emptyMap());
        assertThat(installInvocation.getProcess().waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(installInvocation.getExecutionException()).isNull();
        assertThat(installInvocation.getExitCode()).isEqualTo(0);

    }

    /*
     * This should perhaps be in a different project, see https://github.com/quarkusio/quarkus/issues/46667
     */ @Test
    public void testQuarkusTestInProjectWithJUnitProperties()
            throws MavenInvocationException, InterruptedException {
        String sourceDir = "projects/test-tests-in-project-with-junit-properties-file";
        testDir = initProject(sourceDir, sourceDir + "-processed");
        RunningInvoker invoker = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult installInvocation = invoker.execute(
                List.of("clean", "verify", "-Dquarkus.analytics.disabled=true"),
                Collections.emptyMap());
        assertThat(installInvocation.getProcess().waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(installInvocation.getExecutionException()).isNull();
        assertThat(installInvocation.getExitCode()).isEqualTo(0);

    }

    @Test
    public void testQuarkusTestInProjectWithJUnitPropertiesContinuousTesting()
            throws MavenInvocationException, FileNotFoundException {
        // This test will fail if the test extension does not reset the TCCL properly
        String sourceDir = "projects/test-tests-in-project-with-junit-properties-file";
        testDir = initProject(sourceDir, sourceDir + "-processed-devmode");

        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(1, results.getTestsPassed());
        Assertions.assertEquals(0, results.getTestsSkipped());
    }
}
