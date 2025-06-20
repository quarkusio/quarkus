import org.apache.maven.lifecycle.DefaultLifecycles
import org.apache.maven.lifecycle.Lifecycle
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugin.logging.SystemStreamLog
import org.apache.maven.plugin.testing.AbstractMojoTestCase
import kotlin.test.*

/**
 * Test class demonstrating the LifecyclePhaseAnalyzer functionality and showing
 * how it replaces hardcoded phase behavior with dynamic Maven API analysis.
 */
class LifecyclePhaseAnalyzerTest : AbstractMojoTestCase() {
    
    private lateinit var defaultLifecycles: DefaultLifecycles
    private lateinit var log: Log
    private lateinit var analyzer: LifecyclePhaseAnalyzer
    
    override fun setUp() {
        super.setUp()
        
        // Get DefaultLifecycles from the Maven container
        defaultLifecycles = lookup(DefaultLifecycles::class.java)
        log = SystemStreamLog()
        
        analyzer = LifecyclePhaseAnalyzer(defaultLifecycles, log, true)
    }
    
    override fun tearDown() {
        super.tearDown()
    }
    
    fun testBasicPhaseAnalysis() {
        // Test source processing phase
        val compileAnalysis = analyzer.analyzePhase("compile")
        
        assertNotNull(compileAnalysis)
        assertEquals("compile", compileAnalysis.phase)
        assertEquals("default", compileAnalysis.lifecycleId)
        assertTrue(compileAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.SOURCE_PROCESSING))
        assertTrue(compileAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.COMPILATION))
        assertTrue(compileAnalysis.phasePosition >= 0)
    }
    
    fun testTestPhaseAnalysis() {
        // Test test-related phase
        val testAnalysis = analyzer.analyzePhase("test")
        
        assertNotNull(testAnalysis)
        assertEquals("test", testAnalysis.phase)
        assertEquals("default", testAnalysis.lifecycleId)
        assertTrue(testAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.TEST_RELATED))
    }
    
    fun testResourcePhaseAnalysis() {
        // Test resource processing phase
        val resourceAnalysis = analyzer.analyzePhase("process-resources")
        
        assertNotNull(resourceAnalysis)
        assertEquals("process-resources", resourceAnalysis.phase)
        assertEquals("default", resourceAnalysis.lifecycleId)
        assertTrue(resourceAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.RESOURCE_PROCESSING))
        assertTrue(resourceAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.PROCESSING))
    }
    
    fun testCleanLifecyclePhase() {
        // Test clean lifecycle phase
        val cleanAnalysis = analyzer.analyzePhase("clean")
        
        assertNotNull(cleanAnalysis)
        assertEquals("clean", cleanAnalysis.phase)
        assertEquals("clean", cleanAnalysis.lifecycleId)
        assertTrue(cleanAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.CLEANUP))
    }
    
    fun testSiteLifecyclePhase() {
        // Test site lifecycle phase
        val siteAnalysis = analyzer.analyzePhase("site")
        
        assertNotNull(siteAnalysis)
        assertEquals("site", siteAnalysis.phase)
        assertEquals("site", siteAnalysis.lifecycleId)
        assertTrue(siteAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.DOCUMENTATION))
    }
    
    fun testPhasePositionAnalysis() {
        // Test early phase
        val validateAnalysis = analyzer.analyzePhase("validate")
        assertTrue(validateAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.EARLY_PHASE))
        
        // Test middle phase
        val testAnalysis = analyzer.analyzePhase("test")
        assertTrue(testAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.MIDDLE_PHASE))
        
        // Test late phase
        val deployAnalysis = analyzer.analyzePhase("deploy")
        assertTrue(deployAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.LATE_PHASE))
    }
    
    fun testGoalBehaviorConversion() {
        // Test source processing conversion
        val compileGate = analyzer.toGoalBehavior("compile")
        assertTrue(compileGate.processesSources())
        assertFalse(compileGate.isTestRelated())
        
        // Test test phase conversion
        val testGate = analyzer.toGoalBehavior("test")
        assertTrue(testGate.isTestRelated())
        assertTrue(testGate.processesSources()) // Test phases typically also process sources
        
        // Test resource phase conversion
        val resourceGate = analyzer.toGoalBehavior("process-resources")
        assertTrue(resourceGate.needsResources())
        
        // Test test resource phase conversion
        val testResourceGate = analyzer.toGoalBehavior("process-test-resources")
        assertTrue(testResourceGate.needsResources())
        assertTrue(testResourceGate.isTestRelated())
    }
    
    fun testComplexPhaseNames() {
        // Test phase with multiple semantic indicators
        val testCompileAnalysis = analyzer.analyzePhase("test-compile")
        
        assertTrue(testCompileAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.TEST_RELATED))
        assertTrue(testCompileAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.COMPILATION))
        
        // Test generation phase
        val generateSourcesAnalysis = analyzer.analyzePhase("generate-sources")
        
        assertTrue(generateSourcesAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.GENERATION))
        assertTrue(generateSourcesAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.SOURCE_PROCESSING))
    }
    
    fun testCaching() {
        // Analyze same phase twice
        val first = analyzer.analyzePhase("compile")
        val second = analyzer.analyzePhase("compile")
        
        // Should return same instance due to caching
        assertSame(first, second)
        
        // Verify cache stats
        val stats = analyzer.getCacheStats()
        assertNotNull(stats)
        assertTrue((stats["cachedAnalyses"] as Int) > 0)
    }
    
    fun testGetAllLifecyclePhases() {
        val allPhases = analyzer.getAllLifecyclePhases()
        
        assertNotNull(allPhases)
        assertTrue(allPhases.size > 0)
        assertTrue(allPhases.contains("compile"))
        assertTrue(allPhases.contains("test"))
        assertTrue(allPhases.contains("clean"))
        assertTrue(allPhases.contains("site"))
    }
    
    fun testGetPhasesForLifecycle() {
        val defaultPhases = analyzer.getPhasesForLifecycle("default")
        assertNotNull(defaultPhases)
        assertTrue(defaultPhases.contains("compile"))
        assertTrue(defaultPhases.contains("test"))
        
        val cleanPhases = analyzer.getPhasesForLifecycle("clean")
        assertNotNull(cleanPhases)
        assertTrue(cleanPhases.contains("clean"))
        assertEquals(3, cleanPhases.size)
    }
    
    fun testNullAndEmptyPhases() {
        // Test null phase
        val nullAnalysis = analyzer.analyzePhase(null)
        assertNotNull(nullAnalysis)
        assertNull(nullAnalysis.phase)
        
        // Test empty phase
        val emptyAnalysis = analyzer.analyzePhase("")
        assertNotNull(emptyAnalysis)
        assertEquals("", emptyAnalysis.phase)
    }
    
    fun testUnknownPhase() {
        // Test phase not in any lifecycle
        val unknownAnalysis = analyzer.analyzePhase("unknown-phase")
        
        assertNotNull(unknownAnalysis)
        assertEquals("unknown-phase", unknownAnalysis.phase)
        assertNull(unknownAnalysis.lifecycleId)
        assertEquals(-1, unknownAnalysis.phasePosition)
    }
    
    /**
     * This test demonstrates the key difference between hardcoded and dynamic approaches
     */
    fun testDynamicVsHardcodedComparison() {
        // Test phases that would be handled by hardcoded switch
        val testPhases = arrayOf(
            "compile", "test", "process-resources", "package", 
            "verify", "install", "deploy", "clean"
        )
        
        for (phase in testPhases) {
            val analysis = analyzer.analyzePhase(phase)
            val behavior = analysis.toGoalBehavior()
            
            // Verify dynamic analysis produces reasonable results
            assertNotNull(analysis)
            assertNotNull(behavior)
            
            // Log the analysis for manual verification
            println("Phase: $phase -> ${analysis.summary}")
        }
    }
    
    fun testCacheClear() {
        // Analyze some phases
        analyzer.analyzePhase("compile")
        analyzer.analyzePhase("test")
        
        val statsBefore = analyzer.getCacheStats()
        assertTrue((statsBefore["cachedAnalyses"] as Int) > 0)
        
        // Clear cache
        analyzer.clearCache()
        
        val statsAfter = analyzer.getCacheStats()
        assertEquals(0, statsAfter["cachedAnalyses"] as Int)
    }
}