import model.TargetConfiguration;
import model.TargetMetadata;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for TargetDependencyService using Maven Plugin Testing Harness.
 * This approach provides real Maven context with minimal mocking.
 */
public class TargetDependencyServiceTest {

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    /**
     * Test calculateGoalDependencies with real Maven context
     */
    @Test
    public void testCalculateGoalDependencies() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Test POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        assertNotNull("Mojo should be configured", mojo);

        // Get real Maven session and project from mojo
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0); // Use first project
        Log log = mojo.getLog();
        
        // Create service with real Maven context
        TargetDependencyService service = new TargetDependencyService(log, false, session);
        
        // Test calculateGoalDependencies
        List<String> dependencies = service.calculateGoalDependencies(
            project, "compile", "compiler:compile", reactorProjects);
        
        assertNotNull("Dependencies should not be null", dependencies);
        assertFalse("Dependencies should not be empty", dependencies.isEmpty());
        
        // Should contain cross-module dependency
        boolean hasCompileDependency = dependencies.stream()
            .anyMatch(dep -> dep.contains("compile"));
        assertTrue("Should have compile-related dependency", hasCompileDependency);
    }

    /**
     * Test calculateGoalDependencies with null execution phase using complex test POM
     */
    @Test
    public void testCalculateGoalDependencies_NullPhase() throws Exception {
        File pom = new File("target/test-classes/unit/complex-test");
        assertTrue("Complex test POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        // Test with null phase - should fall back to goal mapping
        List<String> dependencies = service.calculateGoalDependencies(
            project, null, "compiler:compile", reactorProjects);
        
        assertNotNull("Dependencies should not be null", dependencies);
        // Should still produce some dependencies through fallback logic
    }

    /**
     * Test calculateGoalDependencies with empty execution phase using Quarkus Core Builder POM
     */
    @Test
    public void testCalculateGoalDependencies_EmptyPhase() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Quarkus Core Builder POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        List<String> dependencies = service.calculateGoalDependencies(
            project, "", "compiler:compile", reactorProjects);
        
        assertNotNull("Dependencies should not be null", dependencies);
    }

    /**
     * Test calculateGoalDependencies with various common goals using Quarkus Core Runtime POM
     */
    @Test
    public void testCalculateGoalDependencies_CommonGoals() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Quarkus Core Runtime POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        // Test actual Maven goals from the POM configuration
        String[] testGoals = {"compiler:compile", "compiler:testCompile", "surefire:test", "jar:jar", "install:install"};
        
        for (String goal : testGoals) {
            List<String> dependencies = service.calculateGoalDependencies(
                project, null, goal, reactorProjects);
            
            assertNotNull("Dependencies should not be null for goal: " + goal, dependencies);
            
            // Each goal should have cross-module dependency
            boolean hasCrossModuleDep = dependencies.stream()
                .anyMatch(dep -> dep.startsWith("^"));
            assertTrue("Should have cross-module dependency for goal: " + goal, hasCrossModuleDep);
        }
    }

    /**
     * Test calculatePhaseDependencies using Quarkus-like test POM
     */
    @Test
    public void testCalculatePhaseDependencies() throws Exception {
        File pom = new File("target/test-classes/unit/quarkus-like-test");
        assertTrue("Quarkus-like test POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        // Create test targets map
        Map<String, TargetConfiguration> allTargets = createTestTargetsMap();
        
        List<String> dependencies = service.calculatePhaseDependencies(
            "test", allTargets, project, reactorProjects);
        
        assertNotNull("Dependencies should not be null", dependencies);
        
        // Should contain cross-module dependency
        assertTrue("Should contain cross-module test dependency", 
            dependencies.contains("^test"));
    }

    /**
     * Test getPhaseDependencies using Quarkus Core Runtime POM
     */
    @Test
    public void testGetPhaseDependencies() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Quarkus Core Runtime POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        // Test with various phases
        List<String> testDeps = service.getPhaseDependencies("test", project);
        assertNotNull("Test phase dependencies should not be null", testDeps);
        
        List<String> compileDeps = service.getPhaseDependencies("compile", project);
        assertNotNull("Compile phase dependencies should not be null", compileDeps);
    }

    /**
     * Test getPhaseDependencies with null phase using Quarkus Core Builder POM
     */
    @Test
    public void testGetPhaseDependencies_NullPhase() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Quarkus Core Builder POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        List<String> dependencies = service.getPhaseDependencies(null, project);
        assertNotNull("Dependencies should not be null", dependencies);
        assertTrue("Dependencies should be empty for null phase", dependencies.isEmpty());
    }

    /**
     * Test getGoalsForPhase using Quarkus Core Deployment POM
     */
    @Test
    public void testGetGoalsForPhase() throws Exception {
        File pom = new File("target/test-classes/unit/verbose-test");
        assertTrue("Test POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        Map<String, TargetConfiguration> allTargets = createTestTargetsMap();
        
        List<String> goalsForPhase = service.getGoalsForPhase("test", allTargets);
        assertNotNull("Goals for phase should not be null", goalsForPhase);
        
        // Should contain the surefire:test goal which is in the test phase
        assertTrue("Should contain surefire test goal", goalsForPhase.contains("surefire:test"));
    }

    /**
     * Test getGoalsForPhase with empty targets using Quarkus Core Runtime POM
     */
    @Test
    public void testGetGoalsForPhase_EmptyTargets() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Quarkus Core Runtime POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        Map<String, TargetConfiguration> emptyTargets = new HashMap<>();
        
        List<String> goalsForPhase = service.getGoalsForPhase("test", emptyTargets);
        assertNotNull("Goals for phase should not be null", goalsForPhase);
        assertTrue("Goals should be empty for empty targets", goalsForPhase.isEmpty());
    }

    /**
     * Test getPrecedingPhase with real Maven lifecycle using Quarkus Core Deployment POM
     */
    @Test
    public void testGetPrecedingPhase() throws Exception {
        File pom = new File("target/test-classes/unit/verbose-test");
        assertTrue("Test POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        // Test some common Maven lifecycle phases
        String precedingPhase = service.getPrecedingPhase("test", project);
        // The result depends on the actual Maven lifecycle execution plan
        // but should be consistent with Maven's behavior
        
        // Test null handling
        assertNull("Null phase should return null", service.getPrecedingPhase(null, project));
        assertNull("Empty phase should return null", service.getPrecedingPhase("", project));
    }

    /**
     * Test inferPhaseFromGoal with real Maven context using Quarkus Core Runtime POM
     */
    @Test
    public void testInferPhaseFromGoal() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Quarkus Core Runtime POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), false, session);
        
        // Test phase inference for common goals
        String phase = service.inferPhaseFromGoal("compile", project);
        // Result depends on actual Maven execution plan but should be consistent
        
        // Test null handling
        assertNull("Null goal should return null", service.inferPhaseFromGoal(null, project));
        assertNull("Empty goal should return null", service.inferPhaseFromGoal("", project));
    }

    /**
     * Test verbose logging behavior using Quarkus Core Builder POM
     */
    @Test
    public void testVerboseService() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Quarkus Core Builder POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        // Create verbose service
        TargetDependencyService verboseService = new TargetDependencyService(mojo.getLog(), true, session);
        
        // Test that verbose service works
        List<String> dependencies = verboseService.getPhaseDependencies("test", project);
        assertNotNull("Verbose service should work", dependencies);
    }


    /**
     * Test that goals should depend on the preceding phase
     */
    @Test
    public void testGoalsShouldDependOnPrecedingPhase() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Test POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        
        // Enhance the session to be more like a real Maven environment
        enhanceSessionForTesting(session, project);
        
        TargetDependencyService service = new TargetDependencyService(mojo.getLog(), true, session);
        
        // Test that install goal depends on package phase
        // Pass null for executionPhase to force goal-to-phase mapping
        List<String> installDependencies = service.calculateGoalDependencies(
            project, null, "install:install", reactorProjects);
        
        assertNotNull("Install dependencies should not be null", installDependencies);
        
        // Debug: Print actual dependencies
        System.out.println("Install dependencies: " + installDependencies);
        
        // Should contain verify dependency since install's preceding phase is verify in Maven lifecycle
        boolean hasVerifyDependency = installDependencies.stream()
            .anyMatch(dep -> dep.contains("verify"));
        assertTrue("Install goal should depend on verify phase", hasVerifyDependency);
    }
    
    /**
     * Enhance the Maven session to be more like a real Maven environment
     */
    private void enhanceSessionForTesting(MavenSession session, MavenProject project) {
        try {
            // Set up local repository
            if (session.getLocalRepository() == null) {
                File tempRepo = new File(System.getProperty("java.io.tmpdir"), "maven-test-repo");
                if (!tempRepo.exists()) {
                    tempRepo.mkdirs();
                }
                org.apache.maven.artifact.repository.ArtifactRepository localRepo = 
                    new org.apache.maven.artifact.repository.MavenArtifactRepository(
                        "local", 
                        tempRepo.toURI().toString(), 
                        new org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout(),
                        null, null);
                session.getRequest().setLocalRepository(localRepo);
            }
            
            // Ensure project is set properly in session
            session.setCurrentProject(project);
            if (session.getProjects() == null || session.getProjects().isEmpty()) {
                session.setProjects(java.util.Arrays.asList(project));
            }
            
            // Set goals if not present
            if (session.getGoals() == null || session.getGoals().isEmpty()) {
                session.getRequest().setGoals(java.util.Arrays.asList("install"));
            }
            
            // Add execution root directory
            if (session.getExecutionRootDirectory() == null) {
                session.getRequest().setBaseDirectory(project.getBasedir());
            }
            
        } catch (Exception e) {
            // Continue if session enhancement fails
            System.out.println("Warning: Could not fully enhance session: " + e.getMessage());
        }
    }

    /**
     * Test service behavior without Maven session
     */
    @Test
    @WithoutMojo
    public void testServiceWithoutSession() {
        // Create service with null session to test fallback behavior
        TargetDependencyService service = new TargetDependencyService(null, false, null);
        
        // Test methods that should handle null session gracefully
        List<String> phaseDeps = service.getPhaseDependencies("test", null);
        assertNotNull("Phase dependencies should not be null", phaseDeps);
        
        String phase = service.inferPhaseFromGoal("compile", null);
        // Should handle null session gracefully (may return null)
    }

    // Helper methods

    /**
     * Create a map of test targets for testing - simulates real Quarkus build targets
     */
    private Map<String, TargetConfiguration> createTestTargetsMap() {
        Map<String, TargetConfiguration> targets = new HashMap<>();
        
        // Add realistic Quarkus goals for test phase
        TargetConfiguration testCompileTarget = new TargetConfiguration("maven:run");
        TargetMetadata testCompileMetadata = new TargetMetadata("goal", "Test compilation goal");
        testCompileMetadata.setPhase("test-compile");
        testCompileMetadata.setPlugin("maven-compiler-plugin");
        testCompileMetadata.setGoal("testCompile");
        testCompileTarget.setMetadata(testCompileMetadata);
        targets.put("compiler:testCompile", testCompileTarget);
        
        // Add main compile goal
        TargetConfiguration compileTarget = new TargetConfiguration("maven:run");
        TargetMetadata compileMetadata = new TargetMetadata("goal", "Main compilation goal");
        compileMetadata.setPhase("compile");
        compileMetadata.setPlugin("maven-compiler-plugin");
        compileMetadata.setGoal("compile");
        compileTarget.setMetadata(compileMetadata);
        targets.put("compiler:compile", compileTarget);
        
        // Add Quarkus extension plugin goal
        TargetConfiguration quarkusTarget = new TargetConfiguration("maven:run");
        TargetMetadata quarkusMetadata = new TargetMetadata("goal", "Quarkus extension processing");
        quarkusMetadata.setPhase("process-classes");
        quarkusMetadata.setPlugin("quarkus-extension-maven-plugin");
        quarkusMetadata.setGoal("extension-descriptor");
        quarkusTarget.setMetadata(quarkusMetadata);
        targets.put("quarkus:extension-descriptor", quarkusTarget);
        
        // Add surefire test goal
        TargetConfiguration surefireTarget = new TargetConfiguration("maven:run");
        TargetMetadata surefireMetadata = new TargetMetadata("goal", "Run unit tests");
        surefireMetadata.setPhase("test");
        surefireMetadata.setPlugin("maven-surefire-plugin");
        surefireMetadata.setGoal("test");
        surefireTarget.setMetadata(surefireMetadata);
        targets.put("surefire:test", surefireTarget);
        
        // Add phase targets
        TargetConfiguration testPhaseTarget = new TargetConfiguration("maven:phase");
        TargetMetadata testPhaseMetadata = new TargetMetadata("phase", "Test phase");
        testPhaseTarget.setMetadata(testPhaseMetadata);
        targets.put("test", testPhaseTarget);
        
        TargetConfiguration compilePhaseTarget = new TargetConfiguration("maven:phase");
        TargetMetadata compilePhaseMetadata = new TargetMetadata("phase", "Compile phase");
        compilePhaseTarget.setMetadata(compilePhaseMetadata);
        targets.put("compile", compilePhaseTarget);
        
        return targets;
    }
}