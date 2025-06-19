/**
 * Data class representing the behavior and requirements of a Maven goal.
 * Used to replace hardcoded goal classification logic with dynamic analysis.
 */
data class GoalBehavior(
    private var _processesSources: Boolean = false,
    private var _testRelated: Boolean = false,
    private var _needsResources: Boolean = false,
    private var _sourcePaths: MutableList<String> = mutableListOf(),
    private var _resourcePaths: MutableList<String> = mutableListOf(),
    private var _outputPaths: MutableList<String> = mutableListOf()
) {
    // Java-style getters for compatibility
    fun processesSources(): Boolean = _processesSources
    fun isTestRelated(): Boolean = _testRelated
    fun needsResources(): Boolean = _needsResources
    fun getSourcePaths(): MutableList<String> = _sourcePaths
    fun getResourcePaths(): MutableList<String> = _resourcePaths
    fun getOutputPaths(): MutableList<String> = _outputPaths
    
    // Java-style setters for compatibility
    fun setProcessesSources(value: Boolean) { _processesSources = value }
    fun setTestRelated(value: Boolean) { _testRelated = value }
    fun setNeedsResources(value: Boolean) { _needsResources = value }
    fun setSourcePaths(paths: MutableList<String>) { _sourcePaths = paths.toMutableList() }
    fun setResourcePaths(paths: MutableList<String>) { _resourcePaths = paths.toMutableList() }
    fun setOutputPaths(paths: MutableList<String>) { _outputPaths = paths.toMutableList() }
    /**
     * Merge this behavior with another, taking the union of all capabilities.
     */
    fun merge(other: GoalBehavior): GoalBehavior {
        return GoalBehavior(
            _processesSources = this._processesSources || other._processesSources,
            _testRelated = this._testRelated || other._testRelated,
            _needsResources = this._needsResources || other._needsResources,
            _sourcePaths = (this._sourcePaths + other._sourcePaths).toMutableList(),
            _resourcePaths = (this._resourcePaths + other._resourcePaths).toMutableList(),
            _outputPaths = (this._outputPaths + other._outputPaths).toMutableList()
        )
    }
    
    /**
     * Check if this behavior has any defined capabilities.
     */
    fun hasAnyBehavior(): Boolean {
        return _processesSources || _testRelated || _needsResources || 
               _sourcePaths.isNotEmpty() || _resourcePaths.isNotEmpty() || _outputPaths.isNotEmpty()
    }
}