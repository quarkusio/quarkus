/**
 * Data class representing the behavior and requirements of a Maven goal.
 * Used to replace hardcoded goal classification logic with dynamic analysis.
 */
data class GoalBehavior(
    var processesSources: Boolean = false,
    var testRelated: Boolean = false,
    var needsResources: Boolean = false,
    var sourcePaths: MutableList<String> = mutableListOf(),
    var resourcePaths: MutableList<String> = mutableListOf(),
    var outputPaths: MutableList<String> = mutableListOf()
) {
    /**
     * Merge this behavior with another, taking the union of all capabilities.
     */
    fun merge(other: GoalBehavior): GoalBehavior {
        return GoalBehavior(
            processesSources = this.processesSources || other.processesSources,
            testRelated = this.testRelated || other.testRelated,
            needsResources = this.needsResources || other.needsResources,
            sourcePaths = (this.sourcePaths + other.sourcePaths).toMutableList(),
            resourcePaths = (this.resourcePaths + other.resourcePaths).toMutableList(),
            outputPaths = (this.outputPaths + other.outputPaths).toMutableList()
        )
    }
    
    /**
     * Check if this behavior has any defined capabilities.
     */
    fun hasAnyBehavior(): Boolean {
        return processesSources || testRelated || needsResources || 
               sourcePaths.isNotEmpty() || resourcePaths.isNotEmpty() || outputPaths.isNotEmpty()
    }
}