import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.util.*;

/**
 * Test class demonstrating the LifecyclePhaseAnalyzer functionality and showing
 * how it replaces hardcoded phase behavior with dynamic Maven API analysis.
 */
public class LifecyclePhaseAnalyzerTest extends AbstractMojoTestCase {
    
    private DefaultLifecycles defaultLifecycles;
    private Log log;
    private LifecyclePhaseAnalyzer analyzer;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Get DefaultLifecycles from the Maven container
        defaultLifecycles = lookup(DefaultLifecycles.class);
        log = new org.apache.maven.plugin.logging.SystemStreamLog();
        
        analyzer = new LifecyclePhaseAnalyzer(defaultLifecycles, log, true);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    
    public void testBasicPhaseAnalysis() {
        // Test source processing phase
        LifecyclePhaseAnalyzer.PhaseAnalysis compileAnalysis = analyzer.analyzePhase("compile");
        
        assertNotNull(compileAnalysis);
        assertEquals("compile", compileAnalysis.getPhase());
        assertEquals("default", compileAnalysis.getLifecycleId());
        assertTrue(compileAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.SOURCE_PROCESSING));
        assertTrue(compileAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.COMPILATION));
        assertTrue(compileAnalysis.getPhasePosition() >= 0);
    }
    
    public void testTestPhaseAnalysis() {
        // Test test-related phase
        LifecyclePhaseAnalyzer.PhaseAnalysis testAnalysis = analyzer.analyzePhase("test");
        
        assertNotNull(testAnalysis);
        assertEquals("test", testAnalysis.getPhase());
        assertEquals("default", testAnalysis.getLifecycleId());
        assertTrue(testAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.TEST_RELATED));
    }
    
    public void testResourcePhaseAnalysis() {
        // Test resource processing phase
        LifecyclePhaseAnalyzer.PhaseAnalysis resourceAnalysis = analyzer.analyzePhase("process-resources");
        
        assertNotNull(resourceAnalysis);
        assertEquals("process-resources", resourceAnalysis.getPhase());
        assertEquals("default", resourceAnalysis.getLifecycleId());
        assertTrue(resourceAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.RESOURCE_PROCESSING));
        assertTrue(resourceAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.PROCESSING));
    }
    
    public void testCleanLifecyclePhase() {
        // Test clean lifecycle phase
        LifecyclePhaseAnalyzer.PhaseAnalysis cleanAnalysis = analyzer.analyzePhase("clean");
        
        assertNotNull(cleanAnalysis);
        assertEquals("clean", cleanAnalysis.getPhase());
        assertEquals("clean", cleanAnalysis.getLifecycleId());
        assertTrue(cleanAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.CLEANUP));
    }
    
    public void testSiteLifecyclePhase() {
        // Test site lifecycle phase
        LifecyclePhaseAnalyzer.PhaseAnalysis siteAnalysis = analyzer.analyzePhase("site");
        
        assertNotNull(siteAnalysis);
        assertEquals("site", siteAnalysis.getPhase());
        assertEquals("site", siteAnalysis.getLifecycleId());
        assertTrue(siteAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.DOCUMENTATION));
    }
    
    public void testPhasePositionAnalysis() {
        // Test early phase
        LifecyclePhaseAnalyzer.PhaseAnalysis validateAnalysis = analyzer.analyzePhase("validate");
        assertTrue(validateAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.EARLY_PHASE));
        
        // Test middle phase
        LifecyclePhaseAnalyzer.PhaseAnalysis testAnalysis = analyzer.analyzePhase("test");
        assertTrue(testAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.MIDDLE_PHASE));
        
        // Test late phase
        LifecyclePhaseAnalyzer.PhaseAnalysis deployAnalysis = analyzer.analyzePhase("deploy");
        assertTrue(deployAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.LATE_PHASE));
    }
    
    public void testGoalBehaviorConversion() {
        // Test source processing conversion
        GoalBehavior compileGate = analyzer.toGoalBehavior("compile");
        assertTrue(compileGate.processesSources());
        assertFalse(compileGate.isTestRelated());
        
        // Test test phase conversion
        GoalBehavior testGate = analyzer.toGoalBehavior("test");
        assertTrue(testGate.isTestRelated());
        assertTrue(testGate.processesSources()); // Test phases typically also process sources
        
        // Test resource phase conversion
        GoalBehavior resourceGate = analyzer.toGoalBehavior("process-resources");
        assertTrue(resourceGate.needsResources());
        
        // Test test resource phase conversion
        GoalBehavior testResourceGate = analyzer.toGoalBehavior("process-test-resources");
        assertTrue(testResourceGate.needsResources());
        assertTrue(testResourceGate.isTestRelated());
    }
    
    public void testComplexPhaseNames() {
        // Test phase with multiple semantic indicators
        LifecyclePhaseAnalyzer.PhaseAnalysis testCompileAnalysis = analyzer.analyzePhase("test-compile");
        
        assertTrue(testCompileAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.TEST_RELATED));
        assertTrue(testCompileAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.COMPILATION));
        
        // Test generation phase
        LifecyclePhaseAnalyzer.PhaseAnalysis generateSourcesAnalysis = analyzer.analyzePhase("generate-sources");
        
        assertTrue(generateSourcesAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.GENERATION));
        assertTrue(generateSourcesAnalysis.hasCategory(LifecyclePhaseAnalyzer.PhaseCategory.SOURCE_PROCESSING));
    }
    
    public void testCaching() {
        // Analyze same phase twice
        LifecyclePhaseAnalyzer.PhaseAnalysis first = analyzer.analyzePhase("compile");
        LifecyclePhaseAnalyzer.PhaseAnalysis second = analyzer.analyzePhase("compile");
        
        // Should return same instance due to caching
        assertSame(first, second);
        
        // Verify cache stats
        Map<String, Object> stats = analyzer.getCacheStats();
        assertNotNull(stats);
        assertTrue((Integer) stats.get("cachedAnalyses") > 0);
    }
    
    public void testGetAllLifecyclePhases() {
        Set<String> allPhases = analyzer.getAllLifecyclePhases();
        
        assertNotNull(allPhases);
        assertTrue(allPhases.size() > 0);
        assertTrue(allPhases.contains("compile"));
        assertTrue(allPhases.contains("test"));
        assertTrue(allPhases.contains("clean"));
        assertTrue(allPhases.contains("site"));
    }
    
    public void testGetPhasesForLifecycle() {
        List<String> defaultPhases = analyzer.getPhasesForLifecycle("default");
        assertNotNull(defaultPhases);
        assertTrue(defaultPhases.contains("compile"));
        assertTrue(defaultPhases.contains("test"));
        
        List<String> cleanPhases = analyzer.getPhasesForLifecycle("clean");
        assertNotNull(cleanPhases);
        assertTrue(cleanPhases.contains("clean"));
        assertEquals(3, cleanPhases.size());
    }
    
    public void testNullAndEmptyPhases() {
        // Test null phase
        LifecyclePhaseAnalyzer.PhaseAnalysis nullAnalysis = analyzer.analyzePhase(null);
        assertNotNull(nullAnalysis);
        assertNull(nullAnalysis.getPhase());
        
        // Test empty phase
        LifecyclePhaseAnalyzer.PhaseAnalysis emptyAnalysis = analyzer.analyzePhase("");
        assertNotNull(emptyAnalysis);
        assertEquals("", emptyAnalysis.getPhase());
    }
    
    public void testUnknownPhase() {
        // Test phase not in any lifecycle
        LifecyclePhaseAnalyzer.PhaseAnalysis unknownAnalysis = analyzer.analyzePhase("unknown-phase");
        
        assertNotNull(unknownAnalysis);
        assertEquals("unknown-phase", unknownAnalysis.getPhase());
        assertNull(unknownAnalysis.getLifecycleId());
        assertEquals(-1, unknownAnalysis.getPhasePosition());
    }
    
    /**
     * This test demonstrates the key difference between hardcoded and dynamic approaches
     */
    public void testDynamicVsHardcodedComparison() {
        // Test phases that would be handled by hardcoded switch
        String[] testPhases = {
            "compile", "test", "process-resources", "package", 
            "verify", "install", "deploy", "clean"
        };
        
        for (String phase : testPhases) {
            LifecyclePhaseAnalyzer.PhaseAnalysis analysis = analyzer.analyzePhase(phase);
            GoalBehavior behavior = analysis. toGoalBehavior();
            
            // Verify dynamic analysis produces reasonable results
            assertNotNull(analysis);
            assertNotNull(behavior);
            
            // Log the analysis for manual verification
            System.out.println("Phase: " + phase + " -> " + analysis.getSummary());
        }
    }
    
    public void testCacheClear() {
        // Analyze some phases
        analyzer.analyzePhase("compile");
        analyzer.analyzePhase("test");
        
        Map<String, Object> statsBefore = analyzer.getCacheStats();
        assertTrue((Integer) statsBefore.get("cachedAnalyses") > 0);
        
        // Clear cache
        analyzer.clearCache();
        
        Map<String, Object> statsAfter = analyzer.getCacheStats();
        assertEquals((Integer) 0, (Integer) statsAfter.get("cachedAnalyses"));
    }
}