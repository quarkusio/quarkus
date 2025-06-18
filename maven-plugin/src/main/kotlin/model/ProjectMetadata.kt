package model

/**
 * Represents project metadata
 */
data class ProjectMetadata(
    var groupId: String? = null,
    var artifactId: String? = null,
    var version: String? = null,
    var packaging: String? = null,
    var targetGroups: MutableMap<String, List<String>> = linkedMapOf()
) {
    constructor(groupId: String, artifactId: String, version: String, packaging: String) : this() {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
        this.packaging = packaging
    }
    
    fun addTargetGroup(groupName: String, targets: List<String>) {
        targetGroups[groupName] = targets
    }
}