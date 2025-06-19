import org.apache.maven.execution.MavenSession
import org.apache.maven.lifecycle.LifecycleExecutor
import org.apache.maven.lifecycle.DefaultLifecycles
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import model.CreateNodesV2Entry
import model.RawProjectGraphDependency
import model.TargetConfiguration
import model.TargetGroup
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Refactored Maven plugin that analyzes Maven projects for Nx integration.
 * This version delegates complex logic to specialized service classes.
 */
@Mojo(name = "analyze", aggregator = true, requiresDependencyResolution = ResolutionScope.NONE)
class NxAnalyzerMojo : AbstractMojo() {
    
    @Parameter(defaultValue = "\${session}", readonly = true, required = true)
    private lateinit var session: MavenSession
    
    @Parameter(defaultValue = "\${reactorProjects}", readonly = true, required = true)
    private lateinit var reactorProjects: List<MavenProject>
    
    @Parameter(property = "nx.outputFile")
    private var outputFile: String? = null
    
    @Parameter(property = "nx.verbose", defaultValue = "false")
    private var verboseStr: String = "false"
    
    @Component
    private lateinit var lifecycleExecutor: LifecycleExecutor
    
    @Component
    private lateinit var defaultLifecycles: DefaultLifecycles
    
    // Services for delegating complex operations
    private lateinit var executionPlanAnalysisService: ExecutionPlanAnalysisService
    private lateinit var targetGenerationService: TargetGenerationService
    private lateinit var targetGroupService: TargetGroupService
    private lateinit var targetDependencyService: TargetDependencyService
    
    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
        val overallStartTime = System.currentTimeMillis()
        
        log.info("🔥 Starting Maven analysis for ${reactorProjects.size} projects...")
        
        try {
            // Phase 1: Initialize services
            val initStart = System.currentTimeMillis()
            initializeServices()
            val initDuration = System.currentTimeMillis() - initStart
            log.info("⏱️  Service initialization completed in ${initDuration}ms")
            
            logBasicInfo()
            
            // Phase 2: Perform Maven analysis
            val analysisStart = System.currentTimeMillis()
            log.info("📊 Starting Maven workspace analysis...")
            val result = performAnalysis()
            val analysisDuration = System.currentTimeMillis() - analysisStart
            log.info("⏱️  Maven workspace analysis completed in ${analysisDuration}ms (${String.format("%.2f", analysisDuration / 1000.0)}s)")
            
            // Phase 3: Write output
            val writeStart = System.currentTimeMillis()
            val outputPath = determineOutputPath()
            writeResult(result, outputPath)
            val writeDuration = System.currentTimeMillis() - writeStart
            log.info("⏱️  Output file writing completed in ${writeDuration}ms")
            
            logCompletion(overallStartTime, outputPath, analysisDuration)
            
        } catch (e: Exception) {
            val failureDuration = System.currentTimeMillis() - overallStartTime
            log.error("❌ Analysis failed after ${failureDuration}ms: ${e.message}")
            throw MojoExecutionException("Analysis failed: ${e.message}", e)
        }
    }
    
    private fun initializeServices() {
        executionPlanAnalysisService = ExecutionPlanAnalysisService(log, isVerbose(), lifecycleExecutor, session, defaultLifecycles)
        
        // Pre-analyze all projects upfront to avoid performance bottlenecks during dependency analysis
        executionPlanAnalysisService.preAnalyzeAllProjects(reactorProjects)
        
        targetGenerationService = TargetGenerationService(log, isVerbose(), session, executionPlanAnalysisService)
        targetGroupService = TargetGroupService(executionPlanAnalysisService)
        targetDependencyService = TargetDependencyService(log, isVerbose(), executionPlanAnalysisService)
    }
    
    private fun logBasicInfo() {
        if (isVerbose()) {
            log.info("Verbose mode enabled")
        }
        
        log.info("Root directory: ${session.executionRootDirectory}")
        if (reactorProjects.isNotEmpty()) {
            val first = reactorProjects.first()
            val last = reactorProjects.last()
            log.info("First project: ${first.groupId}:${first.artifactId}")
            log.info("Last project: ${last.groupId}:${last.artifactId}")
        }
    }
    
    private fun performAnalysis(): Map<String, Any> {
        val workspaceRoot = File(session.executionRootDirectory)
        
        // Phase 1: Calculate project dependencies
        val depCalcStart = System.currentTimeMillis()
        val projectDependencies = calculateProjectDependencies()
        val depCalcDuration = System.currentTimeMillis() - depCalcStart
        log.info("⏱️  Project dependency calculation completed in ${depCalcDuration}ms")
        
        // Phase 2: Generate targets and groups for each project
        val targetGenStart = System.currentTimeMillis()
        val projectTargets = linkedMapOf<MavenProject, Map<String, TargetConfiguration>>()
        val projectTargetGroups = linkedMapOf<MavenProject, Map<String, TargetGroup>>()
        
        var processedProjects = 0
        for (project in reactorProjects) {
            val projectStart = System.currentTimeMillis()
            try {
                // Get actual project dependencies for this project (not all reactor projects)
                val actualDependencies = projectDependencies[project] ?: emptyList()
                
                // Calculate goal dependencies using only actual dependencies
                val goalDependencies = calculateGoalDependencies(project, actualDependencies)
                
                // Generate targets using pre-calculated goal dependencies (phase dependencies calculated later)
                val targets = targetGenerationService.generateTargets(
                    project, workspaceRoot, goalDependencies, linkedMapOf())
                
                // Now calculate phase dependencies using the generated targets
                val phaseDependencies = calculatePhaseDependencies(project, targets)
                
                // Update phase targets with calculated dependencies
                updatePhaseTargetsWithDependencies(targets, phaseDependencies)
                projectTargets[project] = targets
                
                // Generate target groups
                val targetGroups = targetGroupService.generateTargetGroups(project, targets, session)
                projectTargetGroups[project] = targetGroups
                
                processedProjects++
                val projectDuration = System.currentTimeMillis() - projectStart
                
                if (isVerbose()) {
                    log.info("✅ Processed ${project.artifactId} in ${projectDuration}ms: ${targets.size} targets, ${targetGroups.size} groups, ${actualDependencies.size} project dependencies")
                } else if (processedProjects % 100 == 0) {
                    // Log progress every 100 projects for large workspaces
                    val avgTimePerProject = (System.currentTimeMillis() - targetGenStart) / processedProjects
                    log.info("📊 Processed $processedProjects/${reactorProjects.size} projects (avg ${avgTimePerProject}ms per project)")
                }
                
            } catch (e: Exception) {
                log.error("Error processing project ${project.artifactId}: ${e.message}", e)
                // Continue with empty targets and groups
                projectTargets[project] = linkedMapOf()
                projectTargetGroups[project] = linkedMapOf()
            }
        }
        
        val targetGenDuration = System.currentTimeMillis() - targetGenStart
        log.info("⏱️  Target generation completed in ${targetGenDuration}ms for $processedProjects projects (avg ${targetGenDuration / processedProjects}ms per project)")
        
        // Phase 3: Generate Nx-compatible outputs
        val outputGenStart = System.currentTimeMillis()
        val createNodesEntries = CreateNodesResultGenerator.generateCreateNodesV2Results(
            reactorProjects, workspaceRoot, projectTargets, projectTargetGroups)
        
        val createNodesResults = createNodesEntries.map { it.toArray() }
        
        val createDependencies = CreateDependenciesGenerator.generateCreateDependencies(
            reactorProjects, workspaceRoot, log, isVerbose())
        
        val outputGenDuration = System.currentTimeMillis() - outputGenStart
        log.info("⏱️  Nx output generation completed in ${outputGenDuration}ms")
        
        if (isVerbose()) {
            log.info("Generated ${createDependencies.size} workspace dependencies")
        }
        
        return linkedMapOf<String, Any>(
            "createNodesResults" to createNodesResults,
            "createDependencies" to createDependencies
        )
    }
    
    /**
     * Calculate actual project dependencies for all projects in the reactor.
     * Returns a map from each project to its list of actual Maven dependency projects.
     */
    private fun calculateProjectDependencies(): Map<MavenProject, List<MavenProject>> {
        val projectDependencies = linkedMapOf<MavenProject, List<MavenProject>>()
        
        // Build artifact mapping for workspace projects
        val artifactToProject = hashMapOf<String, MavenProject>()
        for (project in reactorProjects) {
            if (project.groupId != null && project.artifactId != null) {
                val key = MavenUtils.formatProjectKey(project)
                artifactToProject[key] = project
            }
        }
        
        // Calculate dependencies for each project
        for (project in reactorProjects) {
            val dependencies = mutableListOf<MavenProject>()
            
            project.dependencies?.let { deps ->
                for (dep in deps) {
                    if (dep.groupId != null && dep.artifactId != null) {
                        val depKey = "${dep.groupId}:${dep.artifactId}"
                        
                        // Check if this dependency refers to another project in workspace
                        val targetProject = artifactToProject[depKey]
                        if (targetProject != null && targetProject != project) {
                            dependencies.add(targetProject)
                        }
                    }
                }
            }
            
            projectDependencies[project] = dependencies
            
            if (isVerbose() && dependencies.isNotEmpty()) {
                log.debug("Project ${project.artifactId} depends on ${dependencies.size} workspace projects")
            }
        }
        
        return projectDependencies
    }

    /**
     * Calculate goal dependencies for a project using only actual project dependencies
     */
    private fun calculateGoalDependencies(project: MavenProject, actualDependencies: List<MavenProject>): Map<String, List<Any>> {
        val goalDependencies = linkedMapOf<String, List<Any>>()
        
        if (isVerbose()) {
            log.debug("Calculating goal dependencies for ${project.artifactId}")
        }
        
        // First pass: collect all potential goal targets
        val goalTargets = collectGoalTargets(project)
        
        // Calculate dependencies for each goal target
        for (targetName in goalTargets) {
            val goal = ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName)
            
            // Try to find execution phase from plugin configuration
            val executionPhase = findExecutionPhase(project, targetName)
            
            val dependencies = targetDependencyService.calculateGoalDependencies(
                project, executionPhase, targetName, actualDependencies)
            goalDependencies[targetName] = dependencies
        }
        
        return goalDependencies
    }
    
    /**
     * Calculate phase dependencies for a project using generated targets
     */
    private fun calculatePhaseDependencies(project: MavenProject, allTargets: Map<String, TargetConfiguration>): Map<String, List<Any>> {
        val phaseDependencies = linkedMapOf<String, List<Any>>()
        
        // Get all lifecycle phases from all Maven lifecycles (default, clean, site)
        val allPhases = executionPlanAnalysisService.allLifecyclePhases
        
        if (isVerbose()) {
            log.debug("Calculating dependencies for ${allPhases.size} lifecycle phases: $allPhases")
        }
        
        for (phase in allPhases) {
            val dependencies = targetDependencyService.calculatePhaseDependencies(
                phase, allTargets, project, reactorProjects)
            phaseDependencies[phase] = dependencies
        }
        
        return phaseDependencies
    }
    
    /**
     * Update phase targets with calculated dependencies
     */
    private fun updatePhaseTargetsWithDependencies(targets: MutableMap<String, TargetConfiguration>, 
                                                   phaseDependencies: Map<String, List<Any>>) {
        for ((phase, dependencies) in phaseDependencies) {
            val phaseTarget = targets[phase]
            phaseTarget?.dependsOn = dependencies.toMutableList()
        }
    }
    
    /**
     * Collect all potential goal targets from project plugins
     */
    private fun collectGoalTargets(project: MavenProject): Set<String> {
        val goalTargets = linkedSetOf<String>()
        
        if (isVerbose()) {
            log.debug("Collecting goals for ${project.artifactId} (${project.buildPlugins?.size ?: 0} plugins)")
        }
        
        project.buildPlugins?.forEach { plugin ->
            val artifactId = plugin.artifactId
            
            // Add goals from executions
            plugin.executions?.forEach { execution ->
                execution.goals?.forEach { goal ->
                    val targetName = ExecutionPlanAnalysisService.getTargetName(artifactId, goal)
                    goalTargets.add(targetName)
                }
            }
            
            // Add common goals for well-known plugins
            addCommonGoals(artifactId, goalTargets)
        }
        
        if (isVerbose()) {
            log.debug("Found ${goalTargets.size} goals for ${project.artifactId}")
        }
        return goalTargets
    }
    
    /**
     * Find execution phase for a goal target
     */
    private fun findExecutionPhase(project: MavenProject, targetName: String): String? {
        val goal = ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName)
        
        project.buildPlugins?.let { plugins ->
            for (plugin in plugins) {
                plugin.executions?.let { executions ->
                    for (execution in executions) {
                        if (execution.goals?.contains(goal) == true) {
                            return execution.phase
                        }
                    }
                }
            }
        }
        
        return null // Will trigger phase inference in dependency service
    }
    
    /**
     * Add common goals for well-known plugins
     */
    private fun addCommonGoals(artifactId: String, goalTargets: MutableSet<String>) {
        val commonGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin(artifactId)
        for (goal in commonGoals) {
            goalTargets.add(ExecutionPlanAnalysisService.getTargetName(artifactId, goal))
        }
    }
    
    private fun isVerbose(): Boolean {
        val systemProp = System.getProperty("nx.verbose")
        val fromParam = "true".equals(verboseStr, ignoreCase = true)
        val fromSystem = "true".equals(systemProp, ignoreCase = true)
        return fromParam || fromSystem
    }
    
    private fun determineOutputPath(): String {
        return if (!outputFile.isNullOrEmpty()) {
            outputFile!!
        } else {
            File(session.executionRootDirectory, "maven-analysis.json").absolutePath
        }
    }
    
    @Throws(IOException::class)
    private fun writeResult(result: Map<String, Any>, outputPath: String) {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
            
        FileWriter(outputPath).use { writer ->
            gson.toJson(result, writer)
        }
        
        if (isVerbose()) {
            val fileSize = File(outputPath).length()
            log.info("Wrote ${fileSize / 1024}KB analysis results")
        }
    }
    
    private fun logCompletion(startTime: Long, outputPath: String, analysisDuration: Long) {
        val totalTime = System.currentTimeMillis() - startTime
        
        // Calculate analysis percentage of total time
        val analysisPercentage = (analysisDuration * 100.0) / totalTime
        
        // Always show comprehensive timing information
        log.info("✅ Maven analysis completed successfully!")
        log.info("📊 Total execution time: ${String.format("%.2f", totalTime / 1000.0)}s")
        log.info("⚡ Core analysis time: ${String.format("%.2f", analysisDuration / 1000.0)}s (${String.format("%.1f", analysisPercentage)}% of total)")
        log.info("📁 Output written to: $outputPath")
        log.info("🏗️  Projects analyzed: ${reactorProjects.size}")
        
        // Performance insights
        if (reactorProjects.isNotEmpty()) {
            val avgTimePerProject = analysisDuration / reactorProjects.size.toDouble()
            log.info("⏱️  Average time per project: ${String.format("%.1f", avgTimePerProject)}ms")
            
            when {
                avgTimePerProject > 1000 -> {
                    log.warn("⚠️  High average time per project detected - consider optimizing large projects")
                }
                avgTimePerProject < 10 -> {
                    log.info("🚀 Excellent performance - very fast analysis per project")
                }
            }
        }
    }
}