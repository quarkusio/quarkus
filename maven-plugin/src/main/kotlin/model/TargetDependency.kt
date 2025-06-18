package model

/**
 * Represents a target dependency in object form for Nx.
 * This allows for more precise control over dependencies than simple strings.
 * 
 * Based on Nx documentation:
 * - dependencies: Run this target on all dependencies first
 * - projects: Run target on specific projects first
 * - target: target name
 * - params: "forward" or "ignore", defaults to "ignore"
 */
data class TargetDependency(
    var dependencies: Boolean? = null,
    var projects: List<String>? = null,
    var target: String? = null,
    var params: String = "ignore"
) {
    constructor(target: String) : this() {
        this.target = target
    }
    
    constructor(target: String, dependencies: Boolean) : this() {
        this.target = target
        this.dependencies = dependencies
    }
    
    constructor(target: String, projects: List<String>) : this() {
        this.target = target
        this.projects = projects
    }
}