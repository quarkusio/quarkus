package model

/**
 * Represents an Nx project configuration
 */
data class ProjectConfiguration(
    var name: String? = null,
    var root: String? = null,
    var sourceRoot: String? = null,
    var projectType: String? = null,
    var targets: MutableMap<String, TargetConfiguration> = linkedMapOf(),
    var metadata: ProjectMetadata? = null
) {
    constructor(root: String) : this() {
        this.root = root
    }
}