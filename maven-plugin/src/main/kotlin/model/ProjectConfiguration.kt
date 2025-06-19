package model

data class ProjectConfiguration(
    var name: String? = null,
    var root: String? = null,
    var sourceRoot: String? = null,
    var projectType: String? = null,
    var targets: MutableMap<String, TargetConfiguration> = mutableMapOf(),
    var metadata: ProjectMetadata? = null
)