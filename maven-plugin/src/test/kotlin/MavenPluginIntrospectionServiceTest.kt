import org.apache.maven.execution.MavenSession
import org.apache.maven.lifecycle.DefaultLifecycles
import org.apache.maven.lifecycle.LifecycleExecutor
import org.apache.maven.plugin.logging.SystemStreamLog
import org.apache.maven.plugin.testing.AbstractMojoTestCase
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Test Maven plugin introspection APIs to demonstrate dynamic plugin analysis
 */
class MavenPluginIntrospectionServiceTest : AbstractMojoTestCase() {

    fun testIntrospectCompilerPlugin() {
        // Setup test project with compiler plugin
        val pom = getTestFile("src/test/resources/unit/basic-test/pom.xml")
        assertNotNull(pom)
        assertTrue(pom.exists())
        
        // Read the actual POM file
        // Create project and set the POM file for Maven context
        val project = MavenProject()
        project.file = pom
        project.groupId = "test"
        project.artifactId = "basic-test-project"
        project.version = "1.0-SNAPSHOT"
        assertNotNull(project)
        
        // Create basic session
        val session = newMavenSession(project)
        val lifecycleExecutor = lookup(LifecycleExecutor::class.java)
        
        // Create introspection service with system logger
        val service = MavenPluginIntrospectionService(
            session, lifecycleExecutor, SystemStreamLog(), true)
        
        // Test introspection of compile goal
        val result = service.analyzeGoal("compile", project)
        
        assertNotNull("Introspection result should not be null", result)
        assertEquals("Goal should match", "compile", result.goal)
        
        // Compiler plugin should process sources
        assertTrue("Compile goal should process sources", result.processesSources())
        
        // Convert to GoalBehavior
        val behavior = result.toGoalBehavior()
        assertTrue("Behavior should indicate source processing", behavior.processesSources())
        
        println("Compiler plugin introspection result: $result")
        println("File parameters found: ${result.getFileParameters().size}")
        for (param in result.getFileParameters()) {
            println("  - ${param.name} (${param.type})")
        }
    }
    
    fun testIntrospectTestGoal() {
        val pom = getTestFile("src/test/resources/unit/basic-test/pom.xml")
        // Create project and set the POM file for Maven context
        val project = MavenProject()
        project.file = pom
        project.groupId = "test"
        project.artifactId = "basic-test-project"
        project.version = "1.0-SNAPSHOT"
        val session = newMavenSession(project)
        val lifecycleExecutor = lookup(LifecycleExecutor::class.java)
        
        val service = MavenPluginIntrospectionService(
            session, lifecycleExecutor, SystemStreamLog(), true)
        
        // Test introspection of test goal
        val result = service.analyzeGoal("test", project)
        
        assertNotNull("Test introspection result should not be null", result)
        
        val behavior = result.toGoalBehavior()
        assertTrue("Test goal should be test-related", behavior.isTestRelated())
        assertTrue("Test goal should process sources", behavior.processesSources())
        
        println("Test goal introspection result: $result")
        println("Plugin: ${result.pluginGroupId}:${result.pluginArtifactId}")
        println("Phase: ${result.lifecyclePhase}")
        println("Requires project: ${result.isRequiresProject()}")
        println("Dependency resolution: ${result.requiresDependencyResolution}")
    }
    
    fun testEnhancedDynamicAnalysis() {
        val pom = getTestFile("src/test/resources/unit/basic-test/pom.xml")
        // Create project and set the POM file for Maven context
        val project = MavenProject()
        project.file = pom
        project.groupId = "test"
        project.artifactId = "basic-test-project"
        project.version = "1.0-SNAPSHOT"
        val session = newMavenSession(project)
        val lifecycleExecutor = lookup(LifecycleExecutor::class.java)
        val defaultLifecycles = lookup(DefaultLifecycles::class.java)
        
        // Create enhanced analysis service
        val executionPlanService = ExecutionPlanAnalysisService(
            SystemStreamLog(), true, lifecycleExecutor, session, defaultLifecycles)
        
        val enhancedService = EnhancedDynamicGoalAnalysisService(
            session, executionPlanService, lifecycleExecutor, defaultLifecycles, SystemStreamLog(), true)
        
        // Test enhanced analysis
        val compileResult = enhancedService.analyzeGoal("compile", project)
        assertNotNull("Enhanced compile analysis should not be null", compileResult)
        assertTrue("Enhanced analysis should detect source processing", compileResult.processesSources())
        
        val testResult = enhancedService.analyzeGoal("test", project)
        assertNotNull("Enhanced test analysis should not be null", testResult)
        assertTrue("Enhanced analysis should detect test nature", testResult.isTestRelated())
        assertTrue("Enhanced analysis should detect source processing for tests", testResult.processesSources())
        
        // Get detailed introspection result by creating a new service instance
        val introspectionService = MavenPluginIntrospectionService(
            session, lifecycleExecutor, SystemStreamLog(), true)
        val detailedResult = introspectionService.analyzeGoal("compile", project)
        
        println("Enhanced analysis - Compile goal:")
        println("  Processes sources: ${compileResult.processesSources()}")
        println("  Needs resources: ${compileResult.needsResources()}")
        println("  Test related: ${compileResult.isTestRelated()}")
        println("  File parameters: ${detailedResult.getFileParameters().size}")
        println("  Input patterns: ${detailedResult.getInputPatterns()}")
        println("  Output patterns: ${detailedResult.getOutputPatterns()}")
        
        println("Enhanced analysis - Test goal:")
        println("  Processes sources: ${testResult.processesSources()}")
        println("  Needs resources: ${testResult.needsResources()}")
        println("  Test related: ${testResult.isTestRelated()}")
    }
    
    fun testParameterAnalysis() {
        val pom = getTestFile("src/test/resources/unit/basic-test/pom.xml")
        // Create project and set the POM file for Maven context
        val project = MavenProject()
        project.file = pom
        project.groupId = "test"
        project.artifactId = "basic-test-project"
        project.version = "1.0-SNAPSHOT"
        val session = newMavenSession(project)
        val lifecycleExecutor = lookup(LifecycleExecutor::class.java)
        
        val service = MavenPluginIntrospectionService(
            session, lifecycleExecutor, SystemStreamLog(), true)
        
        // Test parameter analysis for different goals
        val goalNames = arrayOf("compile", "test", "package")
        
        for (goalName in goalNames) {
            val result = service.analyzeGoal(goalName, project)
            
            println("\nGoal: $goalName")
            println("Plugin: ${result.pluginArtifactId}")
            println("Description: ${result.description}")
            println("File parameters:")
            
            for (param in result.getFileParameters()) {
                println("  - ${param.name} (${param.type})")
                if (param.description != null && param.description.length < 100) {
                    println("    Description: ${param.description}")
                }
            }
            
            println("Configuration paths:")
            result.getConfigurationPaths().forEach { (key, value) ->
                println("  - $key = $value")
            }
        }
    }
    
    /**
     * This test demonstrates how the introspection service provides much more
     * information than the old hardcoded approach
     */
    fun testComparisonWithHardcodedApproach() {
        val pom = getTestFile("src/test/resources/unit/basic-test/pom.xml")
        // Create project and set the POM file for Maven context
        val project = MavenProject()
        project.file = pom
        project.groupId = "test"
        project.artifactId = "basic-test-project"
        project.version = "1.0-SNAPSHOT"
        val session = newMavenSession(project)
        val lifecycleExecutor = lookup(LifecycleExecutor::class.java)
        
        // Old hardcoded approach (simplified version)
        val hardcodedResult = analyzeGoalHardcodedWay("compile")
        
        // New introspection approach
        val service = MavenPluginIntrospectionService(
            session, lifecycleExecutor, SystemStreamLog(), true)
        val introspectionResult = service.analyzeGoal("compile", project)
        val introspectionBehavior = introspectionResult.toGoalBehavior()
        
        println("Comparison for 'compile' goal:")
        println("Hardcoded approach:")
        println("  Processes sources: ${hardcodedResult.processesSources()}")
        println("  Test related: ${hardcodedResult.isTestRelated()}")
        println("  Needs resources: ${hardcodedResult.needsResources()}")
        println("  Available info: Goal name only")
        
        println("Introspection approach:")
        println("  Processes sources: ${introspectionBehavior.processesSources()}")
        println("  Test related: ${introspectionBehavior.isTestRelated()}")
        println("  Needs resources: ${introspectionBehavior.needsResources()}")
        println("  Plugin: ${introspectionResult.pluginGroupId}:${introspectionResult.pluginArtifactId}")
        println("  Phase: ${introspectionResult.lifecyclePhase}")
        println("  Parameters: ${introspectionResult.getFileParameters().size}")
        println("  Description: ${introspectionResult.description}")
        println("  Requires project: ${introspectionResult.isRequiresProject()}")
        println("  Dependency resolution: ${introspectionResult.requiresDependencyResolution}")
        
        // The introspection approach provides much more detailed information
        assertTrue("Both approaches should agree on basic behavior", 
                  hardcodedResult.processesSources() == introspectionBehavior.processesSources())
    }
    
    /**
     * Simulate old hardcoded approach for comparison
     */
    private fun analyzeGoalHardcodedWay(goal: String): GoalBehavior {
        val behavior = GoalBehavior()
        
        // This is how the old code worked - pure pattern matching
        when (goal) {
            "compile" -> {
                behavior.setProcessesSources(true)
            }
            "testCompile" -> {
                behavior.setProcessesSources(true)
                behavior.setTestRelated(true)
            }
            "test" -> {
                behavior.setTestRelated(true)
                behavior.setProcessesSources(true)
            }
        }
        
        return behavior
    }
}