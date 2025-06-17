import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.plugin.logging.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service that dynamically analyzes Maven lifecycle phases using Maven APIs
 * instead of hardcoded phase categorization. Replaces hardcoded switch statements
 * with intelligent phase analysis based on Maven's lifecycle metadata.
 */
public class LifecyclePhaseAnalyzer {
    
    private final DefaultLifecycles defaultLifecycles;
    private final Log log;
    private final boolean verbose;
    private final Map<String, PhaseAnalysis> analysisCache = new ConcurrentHashMap<>();
    
    public LifecyclePhaseAnalyzer(DefaultLifecycles defaultLifecycles, Log log, boolean verbose) {
        this.defaultLifecycles = defaultLifecycles;
        this.log = log;
        this.verbose = verbose;
    }
    
    /**
     * Analyze a Maven lifecycle phase to determine its behavior and characteristics.
     * Uses Maven's DefaultLifecycles API and semantic analysis instead of hardcoded patterns.
     */
    public PhaseAnalysis analyzePhase(String phase) {
        if (phase == null || phase.isEmpty()) {
            return new PhaseAnalysis(phase);
        }
        
        return analysisCache.computeIfAbsent(phase, this::performPhaseAnalysis);
    }
    
    /**
     * Convert PhaseAnalysis to GoalBehavior for backward compatibility
     */
    public GoalBehavior toGoalBehavior(String phase) {
        PhaseAnalysis analysis = analyzePhase(phase);
        return analysis.toGoalBehavior();
    }
    
    /**
     * Perform comprehensive phase analysis using Maven APIs and semantic analysis
     */
    private PhaseAnalysis performPhaseAnalysis(String phase) {
        if (verbose && log != null) {
            log.debug("Performing dynamic analysis for phase: " + phase);
        }
        
        PhaseAnalysis analysis = new PhaseAnalysis(phase);
        
        // 1. Get lifecycle context using Maven API
        Lifecycle lifecycle = getLifecycleForPhase(phase);
        if (lifecycle != null) {
            analysis.setLifecycleId(lifecycle.getId());
            analysis.setPhasePosition(getPhasePosition(lifecycle, phase));
            analysis.setLifecyclePhases(lifecycle.getPhases());
        }
        
        // 2. Semantic analysis of phase name
        analyzePhaseNameSemantics(analysis);
        
        // 3. Position-based analysis within lifecycle
        analyzePhasePosition(analysis);
        
        // 4. Lifecycle-specific analysis
        analyzeLifecycleContext(analysis);
        
        if (verbose && log != null) {
            log.debug("Phase analysis for '" + phase + "': " + analysis.getSummary());
        }
        
        return analysis;
    }
    
    /**
     * Get lifecycle containing the specified phase using Maven API
     */
    private Lifecycle getLifecycleForPhase(String phase) {
        try {
            if (defaultLifecycles != null) {
                return defaultLifecycles.getPhaseToLifecycleMap().get(phase);
            }
        } catch (Exception e) {
            if (verbose && log != null) {
                log.warn("Could not get lifecycle for phase '" + phase + "': " + e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * Get position of phase within its lifecycle (0-based index)
     */
    private int getPhasePosition(Lifecycle lifecycle, String phase) {
        try {
            List<String> phases = lifecycle.getPhases();
            return phases.indexOf(phase);
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Analyze phase name using semantic patterns (replacement for hardcoded switch)
     */
    private void analyzePhaseNameSemantics(PhaseAnalysis analysis) {
        String phase = analysis.getPhase().toLowerCase();
        
        // Source-related phases
        if (phase.contains("source") || phase.equals("compile") || phase.contains("classes")) {
            analysis.addCategory(PhaseCategory.SOURCE_PROCESSING);
            if (phase.contains("compile")) {
                analysis.addCategory(PhaseCategory.COMPILATION);
            }
        }
        
        // Test-related phases  
        if (phase.contains("test")) {
            analysis.addCategory(PhaseCategory.TEST_RELATED);
            if (phase.contains("compile")) {
                analysis.addCategory(PhaseCategory.COMPILATION);
            }
        }
        
        // Resource-related phases
        if (phase.contains("resource")) {
            analysis.addCategory(PhaseCategory.RESOURCE_PROCESSING);
        }
        
        // Generation phases
        if (phase.contains("generate")) {
            analysis.addCategory(PhaseCategory.GENERATION);
        }
        
        // Processing phases
        if (phase.contains("process")) {
            analysis.addCategory(PhaseCategory.PROCESSING);
        }
        
        // Packaging phases
        if (phase.equals("package") || phase.contains("package")) {
            analysis.addCategory(PhaseCategory.PACKAGING);
        }
        
        // Verification phases
        if (phase.equals("verify") || phase.contains("verify")) {
            analysis.addCategory(PhaseCategory.VERIFICATION);
        }
        
        // Deployment/Installation phases
        if (phase.equals("install") || phase.equals("deploy") || phase.contains("deploy")) {
            analysis.addCategory(PhaseCategory.DEPLOYMENT);
        }
        
        // Validation phases
        if (phase.equals("validate") || phase.contains("validate")) {
            analysis.addCategory(PhaseCategory.VALIDATION);
        }
        
        // Integration test phases
        if (phase.contains("integration")) {
            analysis.addCategory(PhaseCategory.TEST_RELATED);
            analysis.addCategory(PhaseCategory.INTEGRATION);
        }
    }
    
    /**
     * Analyze phase based on its position within the lifecycle
     */
    private void analyzePhasePosition(PhaseAnalysis analysis) {
        if (analysis.getLifecyclePhases() == null || analysis.getPhasePosition() < 0) {
            return;
        }
        
        List<String> phases = analysis.getLifecyclePhases();
        int position = analysis.getPhasePosition();
        int totalPhases = phases.size();
        
        // Early phases (first third) - typically setup/preparation
        if (position < totalPhases / 3) {
            analysis.addCategory(PhaseCategory.EARLY_PHASE);
        }
        // Middle phases - typically compilation/testing  
        else if (position < (2 * totalPhases) / 3) {
            analysis.addCategory(PhaseCategory.MIDDLE_PHASE);
        }
        // Late phases - typically packaging/deployment
        else {
            analysis.addCategory(PhaseCategory.LATE_PHASE);
        }
    }
    
    /**
     * Analyze phase based on its lifecycle context
     */
    private void analyzeLifecycleContext(PhaseAnalysis analysis) {
        String lifecycleId = analysis.getLifecycleId();
        
        if ("clean".equals(lifecycleId)) {
            analysis.addCategory(PhaseCategory.CLEANUP);
        } else if ("site".equals(lifecycleId)) {
            analysis.addCategory(PhaseCategory.DOCUMENTATION);
        } else if ("default".equals(lifecycleId)) {
            analysis.addCategory(PhaseCategory.BUILD);
        }
    }
    
    /**
     * Get all phases from all lifecycles using Maven API
     */
    public Set<String> getAllLifecyclePhases() {
        Set<String> allPhases = new LinkedHashSet<>();
        
        try {
            if (defaultLifecycles != null) {
                for (Lifecycle lifecycle : defaultLifecycles.getLifeCycles()) {
                    if (lifecycle.getPhases() != null) {
                        allPhases.addAll(lifecycle.getPhases());
                    }
                }
            }
        } catch (Exception e) {
            if (verbose && log != null) {
                log.warn("Could not retrieve all lifecycle phases: " + e.getMessage());
            }
        }
        
        return allPhases;
    }
    
    /**
     * Get phases for a specific lifecycle using Maven API
     */
    public List<String> getPhasesForLifecycle(String lifecycleId) {
        try {
            if (defaultLifecycles != null) {
                for (Lifecycle lifecycle : defaultLifecycles.getLifeCycles()) {
                    if (lifecycleId.equals(lifecycle.getId()) && lifecycle.getPhases() != null) {
                        return new ArrayList<>(lifecycle.getPhases());
                    }
                }
            }
        } catch (Exception e) {
            if (verbose && log != null) {
                log.warn("Could not get phases for lifecycle '" + lifecycleId + "': " + e.getMessage());
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Clear analysis cache (useful for testing)
     */
    public void clearCache() {
        analysisCache.clear();
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedAnalyses", analysisCache.size());
        stats.put("analyzedPhases", new ArrayList<>(analysisCache.keySet()));
        return stats;
    }
    
    /**
     * Comprehensive phase analysis result
     */
    public static class PhaseAnalysis {
        private final String phase;
        private String lifecycleId;
        private int phasePosition = -1;
        private List<String> lifecyclePhases;
        private final Set<PhaseCategory> categories = EnumSet.noneOf(PhaseCategory.class);
        
        public PhaseAnalysis(String phase) {
            this.phase = phase;
        }
        
        // Getters and setters
        public String getPhase() { return phase; }
        public String getLifecycleId() { return lifecycleId; }
        public void setLifecycleId(String lifecycleId) { this.lifecycleId = lifecycleId; }
        public int getPhasePosition() { return phasePosition; }
        public void setPhasePosition(int phasePosition) { this.phasePosition = phasePosition; }
        public List<String> getLifecyclePhases() { return lifecyclePhases; }
        public void setLifecyclePhases(List<String> lifecyclePhases) { this.lifecyclePhases = lifecyclePhases; }
        public Set<PhaseCategory> getCategories() { return EnumSet.copyOf(categories.isEmpty() ? EnumSet.noneOf(PhaseCategory.class) : categories); }
        public void addCategory(PhaseCategory category) { categories.add(category); }
        public boolean hasCategory(PhaseCategory category) { return categories.contains(category); }
        
        /**
         * Convert to GoalBehavior for backward compatibility
         */
        public GoalBehavior toGoalBehavior() {
            GoalBehavior behavior = new GoalBehavior();
            
            // Source processing
            if (hasCategory(PhaseCategory.SOURCE_PROCESSING) || hasCategory(PhaseCategory.COMPILATION)) {
                behavior.setProcessesSources(true);
            }
            
            // Test related
            if (hasCategory(PhaseCategory.TEST_RELATED)) {
                behavior.setTestRelated(true);
                // Test phases typically also process sources
                behavior.setProcessesSources(true);
            }
            
            // Resource processing
            if (hasCategory(PhaseCategory.RESOURCE_PROCESSING)) {
                behavior.setNeedsResources(true);
            }
            
            return behavior;
        }
        
        /**
         * Get summary of analysis for logging
         */
        public String getSummary() {
            return String.format("lifecycle=%s, position=%d/%d, categories=%s", 
                lifecycleId, phasePosition, 
                lifecyclePhases != null ? lifecyclePhases.size() : 0,
                categories.stream().map(Enum::name).collect(Collectors.joining(",")));
        }
    }
    
    /**
     * Phase categories determined by dynamic analysis
     */
    public enum PhaseCategory {
        SOURCE_PROCESSING,
        TEST_RELATED,
        RESOURCE_PROCESSING,
        GENERATION,
        PROCESSING,
        COMPILATION,
        PACKAGING,
        VERIFICATION,
        DEPLOYMENT,
        VALIDATION,
        INTEGRATION,
        CLEANUP,
        DOCUMENTATION,
        BUILD,
        EARLY_PHASE,
        MIDDLE_PHASE,
        LATE_PHASE
    }
}